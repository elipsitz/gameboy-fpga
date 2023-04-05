package gameboy.cpu

import chisel3._
import chisel3.util._
import chisel3.experimental.IO
import chisel3.util.experimental.BoringUtils
import gameboy.cpu.TestableCpu.expose

object TestableCpu {
  private def expose[T <: Data](signal: T): T = {
    val ob = IO(Output(chiselTypeOf(signal)))
    ob := 0.U.asTypeOf(chiselTypeOf(signal))
    BoringUtils.bore(signal, Seq(ob))
    ob
  }
}

/** Wrapper of Cpu to expose some internal signals. */
class TestableCpu extends Cpu {
  val xInstructionRegister = expose(instructionRegister)
  val xControlState = expose(control.state)
  val xIme = expose(control.ime)
  val xInstLoad = expose(control.io.instLoad)
  val xDispatchPrefix = expose(control.dispatchPrefix)

  val xRegB = expose(registers(0))
  val xRegC = expose(registers(1))
  val xRegD = expose(registers(2))
  val xRegE = expose(registers(3))
  val xRegH = expose(registers(4))
  val xRegL = expose(registers(5))
  val xRegF = expose(registers(6))
  val xRegA = expose(registers(7))
  val xRegW = expose(registers(10))
  val xRegZ = expose(registers(11))
  val xRegSpHi = expose(registers(8))
  val xRegSpLo = expose(registers(9))
  val xRegPcHi = expose(registers(12))
  val xRegPcLo = expose(registers(13))
}