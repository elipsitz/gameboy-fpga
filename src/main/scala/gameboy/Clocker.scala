package gameboy

import chisel3._

class Clocker extends Bundle {
  /// Clock enable signal.
  val enable = Bool()

  /// 1-clock pulse at the end of a Phi cycle.
  /// 1 MHz regularly, or 2 MHz in double-speed mode.
  val phiPulse = Bool()

  /// 4 MHz pulse (regardless of double-speed mode). Used by PPU and APU
  val pulse4Mhz = Bool()

  // T-cycle counter
  val tCycle = UInt(2.W)
}

class ClockControl extends Module {
  val io = IO(new Bundle {
    /// Global enable signal
    val enable = Input(Bool())

    /// Output clocker signals
    val clocker = Output(new Clocker)
  })

  io.clocker.enable := io.enable

  val tCycle = RegInit(0.U(2.W))
  when (io.enable) {
    tCycle := tCycle + 1.U
  }
  io.clocker.tCycle := tCycle

  io.clocker.phiPulse := io.enable && (tCycle === 3.U)

  // TODO handle double speed
  io.clocker.pulse4Mhz := io.enable
}