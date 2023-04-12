package gameboy.ppu

import chisel3._
import chisel3.util._

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
  })

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
