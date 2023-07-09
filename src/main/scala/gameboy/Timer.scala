package gameboy

import chisel3._
import chisel3.util._
import gameboy.Timer.RegisterControl

object Timer {
  class RegisterControl extends Bundle {
    val enable = Bool()
    val frequency = UInt(2.W)
  }
}

class Timer extends Module {
  val io = IO(new PeripheralAccess {
    val clocker = Input(new Clocker)
    val interruptRequest = Output(Bool())
    /** Bit 4 (5 in double-speed) of DIV, at 512 Hz */
    val divApu = Output(Bool())
    /** From DIV, edges at 16384 Hz (x2 in double speed) */
    val divSerial = Output(Bool())
  })

  // Register definitions
  val regDivider = RegInit(0.U(16.W))
  when (io.clocker.phiPulse) { regDivider := regDivider + 1.U }
  val regCounter = RegInit(0.U(8.W))
  val regModulo = RegInit(0.U(8.W))
  val regControl = RegInit(0.U.asTypeOf(new RegisterControl))
  io.divApu := regDivider(6 + 4)
  io.divSerial := regDivider(5)

  // Timer logic
  val dividerOut = regControl.enable && VecInit(Seq(7, 1, 3, 5).map(regDivider(_)))(regControl.frequency)
  val counterIncrement = RegEnable(dividerOut, false.B, io.clocker.enable) && !dividerOut
  val counterReload = WireDefault(regModulo)
  val counterPostIncrement = regCounter +& 1.U
  val counterOverflowDelay = RegEnable(counterIncrement && counterPostIncrement(8), false.B, io.clocker.enable)
  val counterWritten = WireDefault(false.B)

  // Priority 3/3: memory mapped write to counter
  when (counterIncrement) { regCounter := counterPostIncrement(7, 0) }

  // Register read/write (memory mapped)
  io.dataRead := DontCare
  io.valid := false.B
  when (io.enabled) {
    // 0xFF04 — DIV: Divider register
    when (io.address === 0x04.U) {
      io.valid := true.B
      when (io.write) { regDivider := 0.U }
        .otherwise { io.dataRead := regDivider(13, 6) }
    }
    // FF05 — TIMA: Timer counter
    when (io.address === 0x05.U) {
      io.valid := true.B
      when (io.write) {
        // Priority 2/3: memory mapped write to counter
        regCounter := io.dataWrite
        counterWritten := true.B
      } .otherwise { io.dataRead := regCounter }
    }
    // FF06 — TMA: Timer modulo
    when (io.address === 0x06.U) {
      io.valid := true.B
      when (io.write) {
        regModulo := io.dataWrite
        // Emulate latch
        counterReload := io.dataWrite
      } .otherwise { io.dataRead := regModulo }
    }
    // FF07 — TAC: Timer control
    when (io.address === 0x07.U) {
      io.valid := true.B
      when (io.write) {
        regControl := io.dataWrite(2, 0).asTypeOf(new RegisterControl)
      } .otherwise { io.dataRead := Cat(1.U(5.W), regControl.asUInt) }
    }
  }

  val counterOverflowed = counterOverflowDelay && !counterWritten
  io.interruptRequest := counterOverflowed
  when (counterOverflowed && io.clocker.enable) {
    // Priority 1/3: reload overflowing counter
    regCounter := counterReload
  }
}
