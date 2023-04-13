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
  val scanline = RegInit(0.U(8.W))
  /** Current tick within scanline, [0, 456) */
  val tick = RegInit(0.U(9.W))
  /** Current number of pixels output in this scanline, [0, 160] */
  val pixelsDrawn = RegInit(0.U(8.W))

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
    pixelsDrawn := 0.U
    when (scanline === (Ppu.NumScanlines - 1).U) {
      scanline := 0.U
    } .otherwise {
      scanline := scanline + 1.U
    }
  } .otherwise {
    tick := tick + 1.U
  }

  // 1-hot current state wire
  val stateVblank = scanline >= Ppu.Height.U
  val stateOamSearch = !stateVblank && (tick < Ppu.OamScanLength.U)
  val stateDrawing = !stateVblank && (tick >= Ppu.OamScanLength.U) && (pixelsDrawn < Ppu.Width.U)
  val stateHblank = !stateVblank && (pixelsDrawn === Ppu.Width.U)
  io.output.hblank := stateHblank
  io.output.vblank := stateVblank
  val lycEqualFlag = regLyc === scanline
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
      is (0x44.U) { io.registers.dataRead := scanline }
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

  // Test reading from VRAM
  val testState = RegInit(0.U(8.W))
  val buffHi = RegInit(0.U(8.W))
  val buffLo = RegInit(0.U(8.W))

  val screenX = pixelsDrawn
  val tileX = screenX(6, 3)
  val tileY = scanline(6, 3)
  val tileNum = Cat(tileY, tileX)
  val tileSx = screenX(2, 0)
  val tileSy = scanline(2, 0)
  val tileByte = Cat(tileNum, tileSy, testState === 11.U)

  io.output.pixel := DontCare
  io.output.valid := false.B
  io.vramAddress := tileByte

  when(stateDrawing) {
    when(testState === 0.U) {
      testState := 11.U
    } .otherwise {
      testState := testState - 1.U
    }

    when (testState === 10.U) {
      buffHi := Reverse(io.vramDataRead)
    }
    when (testState === 9.U) {
      buffLo := Reverse(io.vramDataRead)
    }
    when (testState <= 8.U && testState >= 1.U) {
      io.output.valid := true.B
      io.output.pixel := Cat(buffHi(0), buffLo(0))
      buffHi := buffHi >> 1
      buffLo := buffLo >> 1
      pixelsDrawn := pixelsDrawn + 1.U
    }
  } .otherwise {
    testState := 0.U
  }
}
