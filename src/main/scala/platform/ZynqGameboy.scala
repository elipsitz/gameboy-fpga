package platform

import axi.{AxiLiteInitiator, AxiLiteSignals, AxiLiteTarget}
import chisel3._
import chisel3.util._

class ZynqGameboy extends Module {
  val io = IO(new Bundle {
    val leds = Output(UInt(4.W))
    val axiTarget = Flipped(new AxiLiteSignals(8))
    val axiInitiator = new AxiLiteSignals(32)
  })

  val leds = RegInit(0.U(4.W))
  io.leds := leds

  // Reg 0: write -> start address
  // Reg 1: write -> end address (exclusive)
  // Reg 2: write -> set bit 0 to 1 to start
  // Reg 3: read <- sum
  // Reg 4: read -> cycles elapsed
  val registers = RegInit(VecInit(Seq.fill(64)(0.U(32.W))))
  val axiTarget = Module(new AxiLiteTarget(64))
  axiTarget.io.signals <> io.axiTarget
  axiTarget.io.readData := registers
  for (i <- 0 until 8) {
    when (axiTarget.io.writeEnable && axiTarget.io.writeAddr === i.U) {
      registers(i) := axiTarget.io.writeData
    }
  }

  val active = RegInit(false.B)
  val currentAddr = RegInit(0.U(32.W))
  val regAddrStart = registers(0)
  val regAddrEnd = registers(1)
  val regSum = registers(3)
  val regCycles = registers(4)

  val debugIndex = RegInit(0.U(8.W))

  val axiInitiator = Module(new AxiLiteInitiator(32))
  axiInitiator.io.signals <> io.axiInitiator
  axiInitiator.io.read := true.B
  axiInitiator.io.enable := false.B
  axiInitiator.io.address := currentAddr

  when (axiTarget.io.writeEnable && axiTarget.io.writeAddr === 2.U && axiTarget.io.writeData(0)) {
    active := true.B
    currentAddr := regAddrStart
    regSum := 0.U
    regCycles := 0.U
    debugIndex := 0.U
  }

  when (active) {
    regCycles := regCycles + 1.U

    when (!axiInitiator.io.busy) {
      // Start the next read?
      when (currentAddr < regAddrEnd) {
        axiInitiator.io.enable := true.B
        currentAddr := currentAddr + 4.U
      } .otherwise {
        active := false.B
        leds := leds + 1.U
      }

      // We've completed a read.
      when (regCycles =/= 0.U) {
        regSum := regSum + axiInitiator.io.readData

        when (debugIndex < 50.U) {
          registers(8.U + debugIndex) := Cat(regCycles(15, 0), axiInitiator.io.readData(15, 0))
          debugIndex := debugIndex + 1.U
        }
      }
    }
  }
}

object ZynqGameboy extends App {
  emitVerilog(new ZynqGameboy, args)
}