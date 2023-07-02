package gameboy.cart

import chisel3._
import chisel3.util._

class RtcState extends Bundle {
  val seconds = UInt(6.W)
  val minutes = UInt(6.W)
  val hours = UInt(5.W)
  val days = UInt(9.W)
  val halt = Bool()
  val dayOverflow = Bool()
}

/** MBC3 */
class Mbc3 extends Module {
  val io = IO(new MbcIo {
    val hasRtc = Input(Bool())
  })

  val ramEnable = RegInit(false.B)
  val bankRom = RegInit(1.U(7.W))
  val bankRam = RegInit(0.U(4.W))
  val regClockLatch = RegInit(false.B)
  val clockSelect = bankRam(3)

  // RTC
  // Reading always happens from the latched state.
  // Writing (and updates) always happen with the backing state.
  // Writing 0 and then 1 to to latch control will update latched state.
  val rtcState = RegInit(0.U.asTypeOf(new RtcState))
  val rtcStateLatched = RegInit(0.U.asTypeOf(new RtcState))
  // TODO update for handling 8 MHz clock and enable
  val rtcCounter = new Counter(4 * 1024 * 1024)
  when (io.hasRtc && !rtcState.halt) {
    val secondTick = rtcCounter.inc()
    when (secondTick) {
      val nextSeconds = rtcState.seconds + 1.U
      rtcState.seconds := nextSeconds
      when (nextSeconds === 60.U) {
        rtcState.seconds := 0.U
        val nextMinutes = rtcState.minutes + 1.U
        rtcState.minutes := nextMinutes
        when (nextMinutes === 60.U) {
          rtcState.minutes := 0.U
          val nextHours = rtcState.hours + 1.U
          rtcState.hours := nextHours
          when (nextHours === 24.U) {
            rtcState.hours := 0.U
            val nextDays = rtcState.days + 1.U
            rtcState.days := nextDays
            when (nextDays === 0.U) {
              rtcState.dayOverflow := true.B
            }
          }
        }
      }
    }
  }
  // Clock writing
  when (io.memEnable && io.memWrite && !io.selectRom && ramEnable && clockSelect && io.hasRtc) {
    switch (bankRam) {
      is (0x08.U) {
        rtcState.seconds := io.memDataWrite
        // Writing to the seconds register seems to reset the subsecond counter.
        rtcCounter.reset()
      }
      is (0x09.U) { rtcState.minutes := io.memDataWrite }
      is (0x0A.U) { rtcState.hours := io.memDataWrite }
      is (0x0B.U) { rtcState.days := Cat(rtcState.days(8), io.memDataWrite) }
      is (0x0C.U) {
        rtcState.days := Cat(io.memDataWrite(0), rtcState.days(7, 0))
        rtcState.halt := io.memDataWrite(6)
        rtcState.dayOverflow := io.memDataWrite(7)
      }
    }
  }

  when (io.memEnable && io.memWrite && io.selectRom) {
    switch (io.memAddress(14, 13)) {
      is (0.U) { ramEnable := io.memDataWrite(3, 0) === "b1010".U }
      is (1.U) {
        val bank = io.memDataWrite(6, 0)
        bankRom := Mux(bank === 0.U, 1.U, bank)
      }
      is (2.U) { bankRam := io.memDataWrite(3, 0) }
      is (3.U) {
        regClockLatch := io.memDataWrite(0)
        when (!regClockLatch && io.memDataWrite(0)) {
          rtcStateLatched := rtcState
        }
      }
    }
  }

  io.bankRom1 := 0.U
  io.bankRom2 := bankRom
  io.bankRam := bankRam

  // If RAM is disabled, just read 0xFF
  io.memDataRead := 0xFF.U
  io.ramReadMbc := !ramEnable || clockSelect

  // Clock reading.
  when (clockSelect && ramEnable && io.hasRtc) {
    switch (bankRam) {
      is (0x08.U) { io.memDataRead := rtcStateLatched.seconds }
      is (0x09.U) { io.memDataRead := rtcStateLatched.minutes }
      is (0x0A.U) { io.memDataRead := rtcStateLatched.hours }
      is (0x0B.U) { io.memDataRead := rtcStateLatched.days(7, 0) }
      is (0x0C.U) {
        io.memDataRead := Cat(
          rtcStateLatched.dayOverflow,
          rtcStateLatched.halt,
          0.U(5.W),
          rtcStateLatched.days(8)
        )
      }
    }
  }
}
