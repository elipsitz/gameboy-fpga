package gameboy.cart

import chisel3._
import chisel3.util._

/** MBC2 */
class Mbc2 extends Module {
  val io = IO(new MbcIo())

  val ramEnable = RegInit(false.B)
  val bank = RegInit(1.U(4.W))
  val mode = RegInit(false.B)

  when (io.memEnable && io.memWrite && io.selectRom && io.memAddress(14) === 0.U) {
    switch (io.memAddress(8).asUInt) {
      is (0.U) { ramEnable := io.memDataWrite(3, 0) === "b1010".U }
      is (1.U) {
        val index = io.memDataWrite(3, 0)
        bank := Mux(index === 0.U, 1.U, index)
      }
    }
  }

  io.bankRom1 := 0.U
  io.bankRom2 := bank
  io.bankRam := 0.U

  // If RAM is disabled, just read 0xFF
  io.memDataRead := 0xFF.U
  io.ramReadMbc := !ramEnable
}
