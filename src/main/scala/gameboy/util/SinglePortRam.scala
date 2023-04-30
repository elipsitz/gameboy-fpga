package gameboy.util

import chisel3._
import chisel3.util._

/** Synchronous read/write single port (one read *or* one write) RAM */
class SinglePortRam(size: Int) extends Module {
  val io = IO(new Bundle {
    /** Address select */
    val address = Input(UInt(log2Ceil(size).W))
    /** Whether there's an access attempt */
    val enabled = Input(Bool())
    /** Whether the access is a write */
    val write = Input(Bool())
    /** The data being written */
    val dataWrite = Input(UInt(8.W))
    /** The data being read */
    val dataRead = Output(UInt(8.W))
  })

  val ram = SyncReadMem(size, UInt(8.W))

  io.dataRead := 0xFF.U
  when (io.enabled) {
    when (io.write) {
      ram.write(io.address, io.dataWrite)
    } .otherwise {
      io.dataRead := ram.read(io.address)
    }
  }
}
