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

class ClockConfig extends Bundle {
  /// Global clock enable signal.
  val enable = Input(Bool())
  /// Whether an 8 MHz clock is required.
  val need8Mhz = Output(Bool())
  /// True if an 8 Mhz clock is being provided (false for a 4 Mhz one)
  val provide8Mhz = Input(Bool())
}

// TODO: handle double-speed mode
class ClockControl extends Module {
  val io = IO(new Bundle {
    /// Clock configuration
    val clockConfig = new ClockConfig
    /// Output clocker signals
    val clocker = Output(new Clocker)
  })

  io.clockConfig.need8Mhz := false.B

  // Combined tCycle counter and clock divider
  val counter = RegInit(0.U(3.W))
  when (io.clockConfig.enable) {
    counter := counter + Mux(io.clockConfig.provide8Mhz, 1.U, 2.U)
  }

  when (io.clockConfig.provide8Mhz) {
    io.clocker.enable := io.clockConfig.enable && counter(0)
    io.clocker.pulse4Mhz := io.clocker.enable
  } .otherwise {
    io.clocker.enable := io.clockConfig.enable
    io.clocker.pulse4Mhz := io.clocker.enable
  }
  io.clocker.tCycle := counter(2, 1)
  io.clocker.phiPulse := io.clocker.enable && (io.clocker.tCycle === 3.U)
}