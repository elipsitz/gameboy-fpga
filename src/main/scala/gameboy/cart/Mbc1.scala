package gameboy.cart

import chisel3._
import chisel3.util._

/** MBC1 */
class Mbc1 extends Module {
  val io = IO(new MbcIo())

  val ramEnable = RegInit(false.B)
  val bank1 = RegInit(1.U(5.W))
  val bank2 = RegInit(0.U(2.W))
  val mode = RegInit(false.B)

  when (io.memEnable && io.memWrite && io.selectRom) {
    switch (io.memAddress(14, 13)) {
      is (0.U) { ramEnable := io.memDataWrite(3, 0) === "b1010".U }
      is (1.U) {
        val bank = io.memDataWrite(4, 0)
        bank1 := Mux(bank === 0.U, 1.U, bank)
      }
      is (2.U) { bank2 := io.memDataWrite(1, 0) }
      is (3.U) { mode := io.memDataWrite(0) }
    }
  }

  io.bankRom1 := Mux(mode, bank2 << 5, 0.U)
  io.bankRom2 := Cat(bank2, bank1)
  io.bankRam := bank2

  // If RAM is disabled, just read 0xFF
  io.memDataRead := 0xFF.U
  io.ramReadMbc := !ramEnable
}
