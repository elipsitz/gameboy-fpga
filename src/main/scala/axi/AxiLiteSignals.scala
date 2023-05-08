package axi

import chisel3._

/**
 * AXI-4 Lite Signals (from the initiator perspective).
 *
 * @param addrWidth The width of addresses
 * @param dataWidth The width of the data
 */
class AxiLiteSignals(addrWidth: Int, dataWidth: Int = 32) extends Bundle {
  /** Read address channel */
  val arvalid = Output(Bool())
  val arready = Input(Bool())
  val araddr = Output(UInt(addrWidth.W))
  // ARPROT is ignored

  /** Read data channel */
  val rvalid = Input(Bool())
  val rready = Output(Bool())
  val rdata = Input(UInt(dataWidth.W))
  val rresp = Input(UInt(2.W))

  /** Write address channel */
  val awvalid = Output(Bool())
  val awready = Input(Bool())
  val awaddr = Output(UInt(addrWidth.W))
  // AWPROT is ignored

  /** Write data channel */
  val wvalid = Output(Bool())
  val wready = Input(Bool())
  val wdata = Output(UInt(dataWidth.W))
  // WSTRB is ignored

  /** Write response channel */
  val bvalid = Input(Bool())
  val bready = Output(Bool())
  val bresp = Input(UInt(2.W))
}
