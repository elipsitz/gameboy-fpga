package gameboy

import chisel3._
import chisel3.util._
import gameboy.Gameboy.Model
import gameboy.util.MemRomTable

class BootRom(config: Gameboy.Configuration) extends Module {
  val io = IO(new Bundle {
    val peripheral = new PeripheralAccess
    val address = Input(UInt(16.W))
    val valid = Output(Bool())
    val dataRead = Output(UInt(8.W))
  })

  val filename = config.model match {
    case Model.Dmg => "/dmg_boot.bin"
    case Model.Cgb => "/cgb_boot.bin"
  }
  val data = getClass.getResourceAsStream(filename).readAllBytes()
  val rom = Module(new MemRomTable(config,
    UInt(log2Ceil(data.length).W),
    data.map(x => (x & 0xFF).U(8.W)).toIndexedSeq)
  )
  val regBootOff = RegInit(config.skipBootrom.B)

  io.valid := false.B
  io.dataRead := DontCare
  rom.io.addr := DontCare
  when (!regBootOff) {
    when (io.address < 0x100.U) {
      rom.io.addr := io.address
      io.valid := true.B
      io.dataRead := rom.io.data
    }

    if (data.length == 2048) {
      when (io.address >= 0x200.U && io.address < 0x900.U) {
        rom.io.addr := io.address - 0x100.U
        io.valid := true.B
        io.dataRead := rom.io.data
      }
    }
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