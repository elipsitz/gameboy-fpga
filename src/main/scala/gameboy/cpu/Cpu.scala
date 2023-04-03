package gameboy.cpu

import chisel3._

/** Gameboy CPU - Sharp SM83 */
class Cpu extends Module {
  val io = IO(new Bundle {
    /** System bus address selection */
    val memAddress = Output(UInt(16.W))
    /** System bus access enable */
    val memEnable = Output(Bool())
    /** System bus write enable */
    val memWrite = Output(Bool())
    /** System bus data in */
    val memDataIn = Input(UInt(8.W))
    /** System bus data out */
    val memDataOut = Output(UInt(8.W))
  })

  // Clocking: T-Cycles
  val tCycle = RegInit(0.U(2.W))
  tCycle := tCycle + 1.U

  // Control
  val control = Module(new Control())
  control.io.tCycle := tCycle
  control.io.memDataIn := io.memDataIn
  control.io.condition := false.B // TODO: !!! fix!

  // ALU
  val alu = Module(new Alu())


  io.memAddress := 0.U(16.W) // TODO
  io.memEnable := false.B // TODO
  io.memWrite := false.B // TODO
  io.memDataOut := 0.U(8.W) // TODO
}
