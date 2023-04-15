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

/**
 * BG-style FIFO
 * Can be popped from on the same cycle as it's reloaded.
 */
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

  // The logicalRegister thing is sort of like a latch?
  io.reloadAck := length === 0.U && io.reloadEnable
  val logicalRegister = Mux(io.reloadAck, io.reloadData, register)
  val logicalLength = Mux(io.reloadAck, 8.U, length)
  io.outData := logicalRegister(0)
  io.outValid := logicalLength =/= 0.U
  when (logicalLength =/= 0.U && io.popEnable) {
    register := VecInit(logicalRegister.slice(1, 8) ++ Seq(DontCare))
    length := logicalLength - 1.U
  }
}

class BgPixel extends Bundle {
  /** Color */
  val color = UInt(2.W)
}

object FetcherState extends ChiselEnum {
  val id0, id1, lo0, lo1, hi0, hi1 = Value
  // In hi1 we try to push to the FIFO until it succeeds
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
    // TODO mixing
    when (bgFifo.io.outValid) {
      val bgIndex = bgFifo.io.outData.color
      val bgColor = regBgp.colors(bgIndex)
      io.output.pixel := bgColor
      bgFifo.io.popEnable := true.B
      regLx := regLx + 1.U
      io.output.valid := true.B
    }
  }

  // Window registers
  val windowHitWy = RegInit(false.B)
  val windowActive = RegInit(false.B)
  val windowY = Reg(UInt(8.W))

  // Background fetch logic
  io.vramAddress := DontCare
  val fetcherState = RegInit(FetcherState.id0)
  val fetcherTileId = RegInit(0.U(8.W))
  val fetcherTileLo = RegInit(0.U(8.W))
  val fetcherTileHi = RegInit(0.U(8.W))
  val fetcherX = RegInit(0.U(8.W))
  // TODO handle vertical flip
  val fetcherTileAddress = Cat(
    !(regLcdc.bgTileDataArea | fetcherTileId(7)),
    fetcherTileId,
    Mux(windowActive, windowY(2, 0),(regLy + regScy)(2, 0)),
  )
  when (stateDrawing) {
    switch(fetcherState) {
      // For the fetcher, we want to ensure that we can push on 'hi1'.
      // We also need to ensure that fetcherTileId is set by lo0.
      is (FetcherState.id0) {
        // Set tile ID address
        when (windowActive) {
          // Window mode
          val tileY = windowY(7, 3)
          val tileX = fetcherX(7, 3)
          io.vramAddress := Cat("b11".U(2.W), regLcdc.windowTileMapArea, tileY, tileX)
        } .otherwise {
          // Background mode
          val tileY = (regLy + regScy)(7, 3)
          val tileX = (fetcherX + regScx)(7, 3)
          io.vramAddress := Cat("b11".U(2.W), regLcdc.bgTileMapArea, tileY, tileX)
        }
      }
      is (FetcherState.id1) { fetcherTileId := io.vramDataRead }
      is (FetcherState.lo0) { io.vramAddress := Cat(fetcherTileAddress, 0.U(1.W)) }
      is (FetcherState.lo1) {
        // Store this, but also start the address hi fetch so the data is stored by hi1.
        fetcherTileLo := io.vramDataRead
        io.vramAddress := Cat(fetcherTileAddress, 1.U(1.W))
      }
      is (FetcherState.hi0) { fetcherTileHi := io.vramDataRead }
      is (FetcherState.hi1) {
        // Push!
        bgFifo.io.reloadEnable := true.B
        bgFifo.io.reloadData := VecInit(
          (0 until 8).reverse.map(i => Cat(fetcherTileHi(i), fetcherTileLo(i)).asTypeOf(new BgPixel))
        )
      }
    }
    when (fetcherState < FetcherState.hi1) { fetcherState := fetcherState.next }
    when (fetcherState === FetcherState.hi1 && bgFifo.io.reloadAck) {
      fetcherState := FetcherState.id0
      fetcherX := fetcherX + 8.U
    }
  }

  // Window activation logic
  // XXX: windowHitWy should only activate at the beginning of oam search?
  when(stateOamSearch && (regLy === regWy)) { windowHitWy := true.B }
  when(stateDrawing && !windowActive && regLcdc.windowEnable && windowHitWy && regLx >= (regWx - 8.U)) {
    windowActive := true.B
    fetcherState := FetcherState.id0
    fetcherX := 0.U
    bgFifo.reset := true.B
  }

  // On hblank, reset scanline registers
  when(stateHblank) {
    fetcherState := FetcherState.id0
    fetcherX := 0.U
    bgFifo.reset := true.B
    windowActive := false.B
    when (windowActive) {
      windowY := windowY + 1.U
    }
  }
  // On vblank, reset frame registers
  when (stateVblank) {
    windowHitWy := false.B
    windowY := 0.U
  }
}
