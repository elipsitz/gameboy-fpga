package gameboy

import chisel3._
import chisel3.util._

class OamDma extends Module {
  val io = IO(new PeripheralAccess {
    val phiPulse = Input(Bool())
    val active = Output(Bool())
    val dmaAddress = Output(UInt(16.W))
  })

  val sourceHigh = RegInit(0.U(8.W))
  val active = RegInit(false.B)
  val counter = RegInit(0.U(8.W))
  io.active := active
  io.dmaAddress := Cat(sourceHigh, counter)

  io.valid := false.B
  io.dataRead := sourceHigh
  when (io.enabled && io.address === 0x46.U) {
    io.valid := true.B
    when (io.write) {
      // Begin DMA
      sourceHigh := io.dataWrite
      // TODO: there's a short delay before it actually starts
      active := true.B
      counter := 0.U
    }
  }

  when (active && io.phiPulse) {
    when (counter === (160 - 1).U) {
      active := false.B
    } .otherwise {
      counter := counter + 1.U
    }
  }
}