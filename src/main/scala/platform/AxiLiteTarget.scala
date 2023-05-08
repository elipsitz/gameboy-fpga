package platform

import chisel3._
import chisel3.util._

class AxiLiteSignals(addrWidth: Int, dataWidth: Int = 32) extends Bundle {
  /** Read address channel */
  val arvalid = Input(Bool())
  val arready = Output(Bool())
  val araddr = Input(UInt(addrWidth.W))
  // ARPROT is ignored

  /** Read data channel */
  val rvalid = Output(Bool())
  val rready = Input(Bool())
  val rdata = Output(UInt(dataWidth.W))
  val rresp = Output(UInt(2.W))

  /** Write address channel */
  val awvalid = Input(Bool())
  val awready = Output(Bool())
  val awaddr = Input(UInt(addrWidth.W))
  // AWPROT is ignored

  /** Write data channel */
  val wvalid = Input(Bool())
  val wready = Output(Bool())
  val wdata = Input(UInt(dataWidth.W))
  // WSTRB is ignored

  /** Write response channel */
  val bvalid = Output(Bool())
  val bready = Input(Bool())
  val bresp = Output(UInt(2.W))
}

/**
 * Acts as an AXI-Lite target, providing access to some registers.
 */
class AxiLiteTarget(numRegisters: Int) extends Module {
  val addrWidth = log2Ceil(numRegisters * 4)
  val dataWidth = 32
  val io = IO(new Bundle {
    val signals = new AxiLiteSignals(addrWidth)
    val readData = Input(Vec(numRegisters, UInt(dataWidth.W)))
    val writeData = Output(UInt(dataWidth.W))
    val writeEnable = Output(Bool())
    val writeAddr = Output(UInt(addrWidth.W))
  })

  val writeReady = RegInit(false.B)
  io.writeEnable := writeReady
  io.writeAddr := io.signals.awaddr(addrWidth - 1, 2)
  io.writeData := io.signals.wdata

  val readReady = Wire(Bool())
  val readAddr = io.signals.araddr(addrWidth - 1, 2)
  val readData = Reg(UInt(dataWidth.W))
  when (!io.signals.rvalid || io.signals.rready) {
    readData := io.readData(readAddr)
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
