package gameboy.ppu

import chisel3._
import chisel3.util._
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

class Ppu extends Module {
  val io = IO(new Bundle {
    val output = new PpuOutput
    val registers = new PeripheralAccess
  })

  /** $FF40 -- LCDC: LCD Control */
  val regLcdc = RegInit(0.U.asTypeOf(new RegisterLcdControl))
  /** $FF41 -- STAT: LCD status */
  val regStat = RegInit(0.U.asTypeOf(new RegisterStatus))
  /** $FF42 -- SCY: Viewport Y position */
  val regScy = RegInit(0.U(8.W))
  /** $FF43 -- SCX: Viewport X position */
  val regScx = RegInit(0.U(8.W))
  /** $FF44 -- LY: LCD Y coordinate */
  val regLy = RegInit(0.U(8.W))
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

  when (io.registers.enabled && io.registers.write) {
    switch (io.registers.address) {
      is (0x40.U) { regLcdc := io.registers.dataWrite.asTypeOf(new RegisterLcdControl) }
      is (0x41.U) { regStat := io.registers.dataWrite(6, 3).asTypeOf(new RegisterStatus) }
      is (0x42.U) { regScy := io.registers.dataWrite }
      is (0x43.U) { regScx := io.registers.dataWrite }
      is (0x44.U) { regLy := io.registers.dataWrite }
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
        // TODO last 3 bits: (LYC=LY Flag), (Mode Flag)
        io.registers.dataRead := Cat(1.U(1.W), regStat.asUInt, 0.U(3.W))
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

  // Test pattern
  val regTestY = RegInit(0.U(8.W))
  val regTestX = RegInit(0.U(9.W))
  regTestX := regTestX + 1.U
  when (regTestX === 455.U) {
    regTestX := 0.U
    when (regTestY === 153.U) {
      regTestY := 0.U
    } .otherwise {
      regTestY := regTestY + 1.U
    }
  }
  io.output.hblank := regTestX >= 160.U
  io.output.vblank := regTestY >= 144.U
  io.output.valid := !io.output.hblank && !io.output.vblank
  io.output.pixel := regTestX(1, 0)
}
