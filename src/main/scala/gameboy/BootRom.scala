package gameboy

import chisel3._
import chisel3.util._
import gameboy.Gameboy.Model
import gameboy.util.MemRomTable

class BootRom(config: Gameboy.Configuration) extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(16.W))
    val valid = Output(Bool())
    val dataRead = Output(UInt(8.W))

    /** True if the bootrom is mapped. */
    val mapped = Input(Bool())
  })

  val filename = config.model match {
    case Model.Dmg => "/dmg_boot.bin"
    case Model.Cgb => "/cgb_boot.bin"
  }
  var data = getClass.getResourceAsStream(filename).readAllBytes()
  if (data.length == (2048 + 256)) {
    // CGB-style bootrom, but with extra padding in the middle. Remove it.
    data = data.slice(0, 256) ++ data.slice(512, 2048 + 256)
  }
  val rom = Module(new MemRomTable(config,
    UInt(log2Ceil(data.length).W),
    data.map(x => (x & 0xFF).U(8.W)).toIndexedSeq)
  )

  io.valid := false.B
  io.dataRead := DontCare
  rom.io.addr := DontCare
  when (io.mapped) {
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
}