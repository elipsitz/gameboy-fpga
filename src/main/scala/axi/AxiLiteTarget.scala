package axi

import chisel3._
import chisel3.util._

/**
 * Acts as an AXI-Lite target, providing access to some registers.
 */
class AxiLiteTarget(numRegisters: Int) extends Module {
  val indexWidth = log2Ceil(numRegisters)
  val addrWidth = indexWidth + 2
  val dataWidth = 32
  val io = IO(new Bundle {
    val signals = Flipped(new AxiLiteSignals(addrWidth))
    val readIndex = Output(UInt(indexWidth.W))
    val readData = Input(UInt(dataWidth.W))
    val writeData = Output(UInt(dataWidth.W))
    val writeEnable = Output(Bool())
    val writeIndex = Output(UInt(indexWidth.W))
  })

  val writeReady = RegInit(false.B)
  io.writeEnable := writeReady
  io.writeIndex := io.signals.awaddr(addrWidth - 1, 2)
  io.writeData := io.signals.wdata

  val readReady = Wire(Bool())
  io.readIndex := io.signals.araddr(addrWidth - 1, 2)
  val readData = Reg(UInt(dataWidth.W))
  when (!io.signals.rvalid || io.signals.rready) {
    readData := io.readData
  }
  io.signals.rdata := readData

  io.signals.bresp := 0.U
  io.signals.rresp := 0.U

  val bValid = RegInit(false.B)
  io.signals.bvalid := bValid
  when (writeReady) {
    bValid := true.B
  } .elsewhen(io.signals.bready) {
    bValid := false.B
  }

  val readValid = RegInit(false.B)
  io.signals.rvalid := readValid
  when (readReady) {
    readValid := true.B
  } .elsewhen (io.signals.rready) {
    readValid := false.B
  }

  writeReady := !writeReady &&
    (io.signals.awvalid && io.signals.wvalid) &&
    (!io.signals.bvalid || io.signals.bready)
  io.signals.awready := writeReady
  io.signals.wready := writeReady

  io.signals.arready := !io.signals.rvalid
  readReady := io.signals.arvalid && io.signals.arready
}
