package platform

import axi.{AxiLiteInitiator, AxiLiteSignals, AxiLiteTarget}
import chisel3._
import chisel3.util._

class ZynqGameboy extends Module {
  val io = IO(new Bundle {
    val leds = Output(UInt(4.W))
    val axiTarget = Flipped(new AxiLiteSignals(8))
    val axiInitiator = new AxiLiteSignals(32, 64)
  })

  val leds = RegInit(0.U(4.W))
  io.leds := leds

  // Reg 0: write -> start address
  // Reg 1: write -> end address (exclusive)
  // Reg 2: write -> set bit 0 to 1 to start
  // Reg 3: write -> strobe (in the lowest bits)
  // Reg 4: read -> cycles elapsed
  val registers = RegInit(VecInit(Seq.fill(64)(0.U(32.W))))
  val axiTarget = Module(new AxiLiteTarget(64))
  axiTarget.io.signals <> io.axiTarget
  axiTarget.io.readData := registers
  for (i <- 0 until 8) {
    when (axiTarget.io.writeEnable && axiTarget.io.writeIndex === i.U) {
      registers(i) := axiTarget.io.writeData
    }
  }

  val active = RegInit(false.B)
  val currentAddr = RegInit(0.U(32.W))
  val regAddrStart = registers(0)
  val regAddrEnd = registers(1)
  val regStrobe = registers(3)
  val regCycles = registers(4)

//  val debugIndex = RegInit(0.U(8.W))

  val writeData = RegInit(0.U(8.W))

  val axiInitiator = Module(new AxiLiteInitiator(32, 64))
  axiInitiator.io.signals <> io.axiInitiator
  axiInitiator.io.read := false.B
  axiInitiator.io.enable := false.B
  axiInitiator.io.address := Cat(currentAddr(31, 3), 0.U(3.W))
  axiInitiator.io.writeStrobe := regStrobe
  axiInitiator.io.writeData := Cat(
    writeData + 7.U,
    writeData + 6.U,
    writeData + 5.U,
    writeData + 4.U,
    writeData + 3.U,
    writeData + 2.U,
    writeData + 1.U,
    writeData + 0.U,
  )


  when (axiTarget.io.writeEnable && axiTarget.io.writeIndex === 2.U && axiTarget.io.writeData(0)) {
    active := true.B
    currentAddr := regAddrStart
    regCycles := 0.U
  }

  when (active) {
    regCycles := regCycles + 1.U

    when (!axiInitiator.io.busy) {
      // Start the next write?
      when (currentAddr < regAddrEnd) {
        axiInitiator.io.enable := true.B
        currentAddr := currentAddr + 8.U
        writeData := writeData + 1.U
      } .otherwise {
        active := false.B
        leds := leds + 1.U
        writeData := 0.U
      }
    }
  }
}

object ZynqGameboy extends App {
  emitVerilog(new ZynqGameboy, args)
}