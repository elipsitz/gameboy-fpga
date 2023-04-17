package gameboy

import chisel3._
import chisel3.util._

class BootRom(skipBoot: Boolean) extends Module {
  val io = IO(new Bundle {
    val peripheral = new PeripheralAccess
    val address = Input(UInt(16.W))
    val valid = Output(Bool())
    val dataRead = Output(UInt(8.W))
  })

  val data = getClass.getResourceAsStream("/dmg_boot.bin").readAllBytes()
  val rom = VecInit(data.map(x => (x & 0xFF).U(8.W)).toIndexedSeq)
  val regBootOff = RegInit(skipBoot.B)

  io.valid := false.B
  io.dataRead := DontCare
  when (!regBootOff && io.address < 256.U) {
    io.valid := true.B
    io.dataRead := rom(io.address(7, 0))
  }

  io.peripheral.dataRead := Cat("b1111111".U(7.W), regBootOff)
  io.peripheral.valid := false.B
  when (io.peripheral.enabled && io.peripheral.address === 0x50.U) {
    io.peripheral.valid := true.B
    when (io.peripheral.write) {
      regBootOff := regBootOff || io.peripheral.dataWrite(0)
    }
  }
}