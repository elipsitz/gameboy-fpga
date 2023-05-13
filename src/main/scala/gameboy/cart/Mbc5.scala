package gameboy.cart

import chisel3._
import chisel3.util._

/** MBC5 */
class Mbc5 extends Module {
  val io = IO(new MbcIo())

  val ramEnable = RegInit(false.B)
  val bankRomLo = RegInit(1.U(8.W))
  val bankRomHi = RegInit(0.U(1.W))
  val bankRam = RegInit(0.U(4.W))
  val regClockLatch = RegInit(false.B)
  val clockSelect = bankRam(3)

  when (io.memEnable && io.memWrite && io.selectRom) {
    switch (io.memAddress(14, 13)) {
      is (0.U) { ramEnable := io.memDataWrite === "b00001010".U }
      is (1.U) {
        when (!io.memAddress(12)) {
          bankRomLo := io.memDataWrite
        } .otherwise {
          bankRomHi := io.memDataWrite(0)
        }
      }
      is (2.U) { bankRam := io.memDataWrite(3, 0) }
    }
  }

  // TODO support rumble somehow

  io.bankRom1 := 0.U
  io.bankRom2 := Cat(bankRomHi, bankRomLo)
  io.bankRam := bankRam

  // If RAM is disabled, just read 0xFF
  io.memDataRead := 0xFF.U
  io.ramReadMbc := !ramEnable
}
