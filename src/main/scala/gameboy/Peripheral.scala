package gameboy

import chisel3._
import chisel3.util._

class PeripheralAccess extends Bundle {
  /** Peripheral register selection -- 0xFFxx */
  val address = Input(UInt(8.W))
  /** Whether there's an access attempt */
  val enabled = Input(Bool())
  /** Whether the access is a write */
  val write = Input(Bool())
  /** The data being written */
  val dataWrite = Input(UInt(8.W))
  /** The data being read */
  val dataRead = Output(UInt(8.W))
  /** Whether the read is valid */
  val valid = Output(Bool())
}

class HighRam extends Module {
  val io = IO(new PeripheralAccess)

  // But only 127 bytes are accessible
  val memory = SyncReadMem(128, UInt(8.W))
  io.dataRead := 0.U
  io.valid := false.B

  when (io.enabled && io.address >= 0x80.U && io.address <= 0xFE.U) {
    when (io.write) {
      memory.write(io.address(6, 0), io.dataWrite)
    } .otherwise {
      io.valid := true.B
      io.dataRead := memory.read(io.address(6, 0))
    }
  }
}

/** Debug serial output for the simulator */
class DebugSerial extends Module {
  val io = IO(new PeripheralAccess)

  when (io.enabled && io.write && io.address === 0x01.U) {
    printf(cf"${io.dataWrite}%c")
  }

  io.dataRead := DontCare
  io.valid := false.B
}