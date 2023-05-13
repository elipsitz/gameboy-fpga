package axi

import chisel3._

object AxiLiteInitiatorState extends ChiselEnum {
  val idle, readAddr, readData, writeAddrData, writeAddr, writeData, writeResp = Value
}
class AxiLiteInitiator(addrWidth: Int, dataWidth: Int = 32) extends Module {
  val io = IO(new Bundle {
    /** The AXI-Lite signals */
    val signals = new AxiLiteSignals(addrWidth, dataWidth)

    /** Pulsed true to start a memory access. */
    val enable = Input(Bool())
    /** The address to read or write from */
    val address = Input(UInt(addrWidth.W))
    /** Whether this access is a read */
    val read = Input(Bool())
    /** If writing, the data to write. */
    val writeData = Input(UInt(dataWidth.W))
    /** If writing, the write strobe. */
    val writeStrobe = Input(UInt((dataWidth / 8).W))

    /** Whether we're waiting for a memory access to complete. */
    val busy = Output(Bool())
    /** The output data, valid after a read begins and `busy` is false. */
    val readData = Output(UInt(dataWidth.W))
  })

  val state = RegInit(AxiLiteInitiatorState.idle)

  // Handle starting a memory access
  val address = Reg(UInt(addrWidth.W))
  val writeStrobe = Reg(UInt((dataWidth / 8).W))
  val writeData = Reg(UInt(dataWidth.W))
  when (state === AxiLiteInitiatorState.idle && io.enable) {
    address := io.address
    when (io.read) {
      state := AxiLiteInitiatorState.readAddr
    } .otherwise {
      writeStrobe := io.writeStrobe
      writeData := io.writeData
      state := AxiLiteInitiatorState.writeAddrData
    }
  }

  // Pass through addr on enable and idle, or when waiting for readAddr
  io.signals.araddr := Mux(state === AxiLiteInitiatorState.idle, io.address, address)
  io.signals.arvalid :=
    (state === AxiLiteInitiatorState.idle && io.enable && io.read) ||
    (state === AxiLiteInitiatorState.readAddr)

  when (io.signals.arvalid && io.signals.arready) {
    state := AxiLiteInitiatorState.readData
  }

  // Write addr/data
  io.signals.awaddr := Mux(state === AxiLiteInitiatorState.idle, io.address, address)
  io.signals.awvalid :=
    (state === AxiLiteInitiatorState.idle && io.enable && !io.read) ||
    (state === AxiLiteInitiatorState.writeAddrData || state === AxiLiteInitiatorState.writeAddr)
  io.signals.wdata := Mux(state === AxiLiteInitiatorState.idle, io.writeData, writeData)
  io.signals.wstrb := Mux(state === AxiLiteInitiatorState.idle, io.writeStrobe, writeStrobe)
  io.signals.wvalid :=
    (state === AxiLiteInitiatorState.idle && io.enable && !io.read) ||
    (state === AxiLiteInitiatorState.writeAddrData || state === AxiLiteInitiatorState.writeData)

  // State transition to write response
  val writeAddrComplete = io.signals.awvalid && io.signals.awready
  val writeDataComplete = io.signals.wvalid && io.signals.wready
  when (state === AxiLiteInitiatorState.writeAddrData) {
    when (writeAddrComplete && writeDataComplete) { state := AxiLiteInitiatorState.writeResp }
    .elsewhen (writeAddrComplete) { state := AxiLiteInitiatorState.writeData }
    .elsewhen (writeDataComplete) { state := AxiLiteInitiatorState.writeAddr }
  }
  when (state === AxiLiteInitiatorState.writeAddr && writeAddrComplete) {
    state := AxiLiteInitiatorState.writeResp
  }
  when(state === AxiLiteInitiatorState.writeData && writeDataComplete) {
    state := AxiLiteInitiatorState.writeResp
  }

  // Read data -- ignore the read response
  val readData = Reg(UInt(dataWidth.W))
  io.readData := readData
  when (io.signals.rvalid) {
    readData := io.signals.rdata
    state := AxiLiteInitiatorState.idle
  }
  io.signals.rready := true.B

  // Write response (ignore it)
  when (io.signals.bvalid) {
    state := AxiLiteInitiatorState.idle
  }
  io.signals.bready := true.B

  io.busy := state =/= AxiLiteInitiatorState.idle
}
