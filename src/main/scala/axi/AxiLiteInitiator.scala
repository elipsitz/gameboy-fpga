package axi

import chisel3._

object AxiLiteInitiatorState extends ChiselEnum {
  val idle, readAddr, readData = Value
}
class AxiLiteInitiator(addrWidth: Int, dataWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    /** The AXI-Lite signals */
    val signals = new AxiLiteSignals(addrWidth)

    /** Pulsed true to start a memory access. */
    val enable = Input(Bool())
    /** The address to read or write from */
    val address = Input(UInt(addrWidth.W))
    /** Whether this access is a read */
    val read = Input(Bool())
    // TODO: the write data

    /** Whether we're waiting for a memory access to complete. */
    val busy = Output(Bool())
    /** The output data, valid after a read begins and `busy` is false. */
    val readData = Output(UInt(dataWidth.W))
  })

  val state = RegInit(AxiLiteInitiatorState.idle)

  // Handle starting a memory access
  val address = Reg(UInt(addrWidth.W))
  when (state === AxiLiteInitiatorState.idle && io.enable && io.read) {
    address := io.address
    state := AxiLiteInitiatorState.readAddr
  }

  // Pass through addr on enable and idle, or when waiting for readAddr
  io.signals.araddr := Mux(state === AxiLiteInitiatorState.idle, io.address, address)
  io.signals.arvalid :=
    (state === AxiLiteInitiatorState.idle && io.enable && io.read) ||
    (state === AxiLiteInitiatorState.readAddr)

  when (io.signals.arvalid && io.signals.arready) {
    state := AxiLiteInitiatorState.readData
  }

  // Read data -- ignore the read response
  val readData = Reg(UInt(dataWidth.W))
  io.readData := readData
  when (io.signals.rvalid) {
    readData := io.signals.rdata
    state := AxiLiteInitiatorState.idle
  }
  io.signals.rready := true.B

  io.busy := state =/= AxiLiteInitiatorState.idle

  // Currently unused signals
  io.signals.awvalid := false.B
  io.signals.awaddr := DontCare
  io.signals.wvalid := false.B
  io.signals.wdata := DontCare
  io.signals.bready := false.B
}
