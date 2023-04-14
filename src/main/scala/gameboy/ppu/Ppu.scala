package gameboy.ppu

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import gameboy.PeripheralAccess

class PpuOutput extends Bundle {
  /** Output pixel value */
  val pixel = Output(UInt(2.W))
  /** Whether the pixel this clock is valid */
  val valid = Output(Bool())
  /** Whether the PPU is in hblank */
  val hblank = Output(Bool())
  /** Whether the PPU is in vblank */
  val vblank = Output(Bool())
}

object Ppu {
  val Width = 160
  val Height = 144
  val NumScanlines = 154
  val NumTicks = 456
  val OamScanLength = 80
  val FifoSize = 8
}

class PixelFifo[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle {
    val reloadData = Input(Vec(8, gen))
    val reloadEnable = Input(Bool())
    val popEnable = Input(Bool())
    val outData = Output(gen)
    val outValid = Output(Bool())
    val reloadAck = Output(Bool())
  })

  val register = Reg(Vec(8, gen))
  val length = RegInit(0.U(4.W))
  io.outData := register(0)
  io.outValid := length =/= 0.U

  io.reloadAck := false.B
  when (io.reloadEnable && (length === 0.U || (io.popEnable && length === 1.U))) {
    register := io.reloadData
    io.reloadAck := true.B
    length := 8.U
  } .elsewhen (io.popEnable && length > 0.U) {
    register := VecInit(register.slice(1, 8) ++ Seq(DontCare))
    length := length - 1.U
  }
}

class BgPixel extends Bundle {
  /** Color */
  val color = UInt(2.W)
}

object FetcherState extends ChiselEnum {
  val id0, id1, lo0, lo1, hi0, hi1, push = Value
  // In hi1 and push we try to push to the FIFO
}

class Ppu extends Module {
  val io = IO(new Bundle {
    val output = new PpuOutput
    val registers = new PeripheralAccess

    // IRQs
    val vblankIrq = Output(Bool())
    val statIrq = Output(Bool())

    // VRAM access
    /** Whether the PPU is accessing VRAM */
    val vramEnabled = Output(Bool())
    /** Which VRAM address the PPU is accessing */
    val vramAddress = Output(UInt(13.W))
    /** Data read from VRAM (synchronous, in the next clock cycle) */
    val vramDataRead = Input(UInt(8.W))
  })

  /** Current scanline, [0, 154) */
  val regLy = RegInit(0.U(8.W))
  /** Current tick within scanline, [0, 456) */
  val tick = RegInit(0.U(9.W))
  /** Current number of pixels output in this scanline, [0, 160] */
  val regLx = RegInit(0.U(8.W))

  /** $FF40 -- LCDC: LCD Control */
  val regLcdc = RegInit(0.U.asTypeOf(new RegisterLcdControl))
  /** $FF41 -- STAT: LCD status */
  val regStat = RegInit(0.U.asTypeOf(new RegisterStatus))
  /** $FF42 -- SCY: Viewport Y position */
  val regScy = RegInit(0.U(8.W))
  /** $FF43 -- SCX: Viewport X position */
  val regScx = RegInit(0.U(8.W))
  /** $FF45 -- LYC: LY compare */
  val regLyc = RegInit(0.U(8.W))
  /** $FF47 -- BGP: BG Palette data */
  val regBgp = RegInit(0.U.asTypeOf(new RegisterPalette))
  /** $FF48 -- OBP0: OBJ Palette 0 data */
  val regObp0 = RegInit(0.U.asTypeOf(new RegisterPalette))
  /** $FF49 -- OBP1: OBJ Palette 1 data */
  val regObp1 = RegInit(0.U.asTypeOf(new RegisterPalette))
  /** $FF4A -- WY: Window Y position */
  val regWy = RegInit(0.U(8.W))
  /** $FF4B -- WX: Window X position (plus 7) */
  val regWx = RegInit(0.U(8.W))

  // Tick and scanline adjustment
  when (tick === (Ppu.NumTicks - 1).U) {
    tick := 0.U
    regLx := 0.U
    when (regLy === (Ppu.NumScanlines - 1).U) {
      regLy := 0.U
    } .otherwise {
      regLy := regLy + 1.U
    }
  } .otherwise {
    tick := tick + 1.U
  }

  // 1-hot current state wire
  val stateVblank = regLy >= Ppu.Height.U
  val stateOamSearch = !stateVblank && (tick < Ppu.OamScanLength.U)
  val stateDrawing = !stateVblank && (tick >= Ppu.OamScanLength.U) && (regLx < Ppu.Width.U)
  val stateHblank = !stateVblank && (regLx === Ppu.Width.U)
  io.output.hblank := stateHblank
  io.output.vblank := stateVblank
  val lycEqualFlag = regLyc === regLy
  io.vramEnabled := stateDrawing && regLcdc.enable

  // Memory-mapped register access
  when (io.registers.enabled && io.registers.write) {
    switch (io.registers.address) {
      is (0x40.U) { regLcdc := io.registers.dataWrite.asTypeOf(new RegisterLcdControl) }
      is (0x41.U) { regStat := io.registers.dataWrite(6, 3).asTypeOf(new RegisterStatus) }
      is (0x42.U) { regScy := io.registers.dataWrite }
      is (0x43.U) { regScx := io.registers.dataWrite }
      is (0x45.U) { regLyc := io.registers.dataWrite }
      // 0x46 -- DMA -- is *not* included
      is (0x47.U) { regBgp := io.registers.dataWrite.asTypeOf(new RegisterPalette) }
      is (0x48.U) { regObp0 := io.registers.dataWrite.asTypeOf(new RegisterPalette) }
      is (0x49.U) { regObp1 := io.registers.dataWrite.asTypeOf(new RegisterPalette) }
      is (0x4A.U) { regWy := io.registers.dataWrite }
      is (0x4B.U) { regWx := io.registers.dataWrite }
    }
  }
  io.registers.dataRead := 0xFF.U
  when (io.registers.enabled && !io.registers.write) {
    switch (io.registers.address) {
      is (0x40.U) { io.registers.dataRead := regLcdc.asUInt }
      is (0x41.U) {
        val mode = OHToUInt(Seq(stateHblank, stateVblank, stateOamSearch, stateDrawing))
        io.registers.dataRead := Cat(1.U(1.W), regStat.asUInt, lycEqualFlag, mode)
      }
      is (0x42.U) { io.registers.dataRead := regScy }
      is (0x43.U) { io.registers.dataRead := regScx }
      is (0x44.U) { io.registers.dataRead := regLy }
      is (0x45.U) { io.registers.dataRead := regLyc }
      is (0x47.U) { io.registers.dataRead := regBgp.asUInt }
      is (0x48.U) { io.registers.dataRead := regObp0.asUInt }
      is (0x49.U) { io.registers.dataRead := regObp1.asUInt }
      is (0x4A.U) { io.registers.dataRead := regWy }
      is (0x4B.U) { io.registers.dataRead := regWx }
    }
  }
  io.registers.valid :=
    VecInit(
      Seq(0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x47, 0x48, 0x49, 0x4A, 0x4B)
        .map(io.registers.address === _.U)
    ).asUInt.orR

  // Interrupt request generation
  io.vblankIrq := !ShiftRegister(stateVblank, 1, false.B, true.B) && stateVblank
  val statInterrupt = Cat(Seq(
    (lycEqualFlag && regStat.interruptLyEnable),
    (stateOamSearch && regStat.interruptOamEnable),
    (stateVblank && regStat.interruptVblankEnable),
    (stateHblank && regStat.interruptHblankEnable),
  )).orR
  io.statIrq := !ShiftRegister(statInterrupt, 1, false.B, true.B) && statInterrupt

  // Background FIFO
  val bgFifo = Module(new PixelFifo(new BgPixel))
  bgFifo.io.popEnable := false.B
  bgFifo.io.reloadEnable := false.B
  bgFifo.io.reloadData := DontCare

  // Output pixel logic
  io.output.pixel := DontCare
  io.output.valid := false.B
  when (stateDrawing) {
    when (bgFifo.io.outValid) {
      io.output.pixel := bgFifo.io.outData.color
      bgFifo.io.popEnable := true.B
      regLx := regLx + 1.U
      io.output.valid := true.B
    }
  }

  // Background fetch logic
  io.vramAddress := DontCare
  val fetcherState = RegInit(FetcherState.id0)
  val fetcherTileId = RegInit(0.U(8.W))
  val fetcherTileLo = RegInit(0.U(8.W))
  val fetcherTileHi = RegInit(0.U(8.W))
  // TODO handle vertical flip
  val fetcherTileAddress = Cat(
    !(regLcdc.bgTileDataArea | fetcherTileId(7)),
    fetcherTileId,
    (regLy + regScy)(2, 0),
  )
  when (stateDrawing) {
    switch(fetcherState) {
      // Set tilemap address
      is (FetcherState.id0) {
        // TODO handle window mode
        val tileY = (regLy + regScy)(7, 3)
        val tileX = (regLx + regScx)(7, 3)
        io.vramAddress := Cat("b11".U(2.W), regLcdc.bgTileMapArea, tileY, tileX)
        fetcherState := FetcherState.id1
      }
      // Store tilemap address
      is (FetcherState.id1) {
        fetcherTileId := io.vramDataRead
        fetcherState := FetcherState.lo0
      }
      // Set tile low address
      is (FetcherState.lo0) {
        io.vramAddress := Cat(fetcherTileAddress, 0.U(1.W))
        fetcherState := FetcherState.lo1
      }
      // Store tile low data
      is (FetcherState.lo1) {
        fetcherTileLo := io.vramDataRead
        fetcherState := FetcherState.hi0
      }
      // Set tile high address
      is (FetcherState.hi0) {
        io.vramAddress := Cat(fetcherTileAddress, 1.U(1.W))
        fetcherState := FetcherState.hi1
      }
      // Store tile high data
      is (FetcherState.hi1) {
        fetcherTileHi := io.vramDataRead
        fetcherState := FetcherState.push
        bgFifo.io.reloadEnable := true.B
        bgFifo.io.reloadData := VecInit(
          (0 until 8).reverse.map(i => Cat(io.vramDataRead(i), fetcherTileLo(i)).asTypeOf(new BgPixel))
        )
        when (bgFifo.io.reloadAck) {
          fetcherState := FetcherState.id0
        }
      }
      is (FetcherState.push) {
        // Waiting for push... don't update fetcherState, wait here until the push is done
        bgFifo.io.reloadEnable := true.B
        bgFifo.io.reloadData := VecInit(
          (0 until 8).reverse.map(i => Cat(fetcherTileHi(i), fetcherTileLo(i)).asTypeOf(new BgPixel))
        )
        when (bgFifo.io.reloadAck) {
          fetcherState := FetcherState.id0
        }
      }
    }
  }

  // On hblank, reset scanline registers
  when(stateHblank) {
    fetcherState := FetcherState.id0
    bgFifo.reset := true.B
  }
}
