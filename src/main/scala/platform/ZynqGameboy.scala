package platform

import chisel3._
import chisel3.util._

class ZynqGameboy extends Module {
  val io = IO(new Bundle {
    val leds = Output(UInt(4.W))
    val axiTarget = new AxiLiteSignals(5)
  })

  val registers = RegInit(VecInit(Seq.fill(8)(0.U(32.W))))
  val axiTarget = Module(new AxiLiteTarget(8))
  axiTarget.io.signals <> io.axiTarget
  axiTarget.io.readData := registers
  for (i <- 0 until 7) {
    when (axiTarget.io.writeEnable && axiTarget.io.writeAddr === i.U) {
      registers(i) := axiTarget.io.writeData
    }
  }

  // Write counter
  when (axiTarget.io.writeEnable) {
    registers(7) := registers(7) + 1.U
  }

  io.leds := VecInit((0 until 4).map(registers(_)(0))).asUInt
}

object ZynqGameboy extends App {
  emitVerilog(new ZynqGameboy, args)
}