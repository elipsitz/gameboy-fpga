package gameboy.cart

import chisel3._
import chisel3.util._

/** MBC3 */
class Mbc3 extends Module {
  val io = IO(new MbcIo())

  val ramEnable = RegInit(false.B)
  val bankRom = RegInit(1.U(7.W))
  val bankRam = RegInit(0.U(4.W))
  val regClockLatch = RegInit(false.B)
  val clockSelect = bankRam(3)

  when (io.memEnable && io.memWrite && io.selectRom) {
    switch (io.memAddress(14, 13)) {
      is (0.U) { ramEnable := io.memDataWrite(3, 0) === "b1010".U }
      is (1.U) {
        val bank = io.memDataWrite(6, 0)
        bankRom := Mux(bank === 0.U, 1.U, bank)
      }
      is (2.U) { bankRam := io.memDataWrite(3, 0) }
      is (3.U) { regClockLatch := io.memDataWrite(0) /** TODO */ }
    }
  }

  io.bankRom1 := 0.U
  io.bankRom2 := bankRom
  io.bankRam := bankRam

  // If RAM is disabled, just read 0xFF
  io.memDataRead := 0xFF.U
  io.ramReadMbc := !ramEnable || clockSelect
  // TODO: implement clock
}
