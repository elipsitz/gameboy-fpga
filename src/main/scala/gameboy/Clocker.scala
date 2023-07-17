package gameboy

import chisel3._

class Clocker extends Bundle {
  /// Clock enable signal. 4 Mhz normally, or 8 Mhz in double-speed mode.
  val enable = Bool()

  /// 1-clock pulse at the end of a Phi cycle.
  /// 1 MHz regularly, or 2 MHz in double-speed mode.
  val phiPulse = Bool()

  /// 4 MHz pulse (regardless of double-speed mode). Used by PPU and APU
  val pulse4Mhz = Bool()

  /// T-cycle counter
  val tCycle = UInt(2.W)

  /// True if we're in double-speed mode
  val doubleSpeed = Bool()

  /// For VRAM DMA: 2 MHz pulse at the end of a cycle (double speed) or half-cycle (single speed).
  val pulseVramDma = Bool()

  /// The 8 Mhz cycle counter (regardless of double speed)
  val counter8Mhz = UInt(2.W)
}

class ClockConfig extends Bundle {
  /// Global clock enable signal.
  val enable = Input(Bool())
  /// Whether an 8 MHz clock is required.
  val need8Mhz = Output(Bool())
  /// True if an 8 Mhz clock is being provided (false for a 4 Mhz one)
  val provide8Mhz = Input(Bool())
}

class ClockControl extends Module {
  val io = IO(new Bundle {
    /// Clock configuration
    val clockConfig = new ClockConfig
    /// Output clocker signals
    val clocker = Output(new Clocker)
    /// Whether we're in double-speed mode
    val doubleSpeed = Input(Bool())
    /// Whether CGB VRAM DMA is active.
    val vramDmaActive = Input(Bool())
  })

  io.clockConfig.need8Mhz := io.doubleSpeed || io.vramDmaActive
  io.clocker.doubleSpeed := io.doubleSpeed

  // Combined tCycle counter and clock divider
  val counter = RegInit(0.U(3.W))
  when (io.clockConfig.enable) {
    // Assume the provided clock is 8 MHz if we're in double-speed mode.
    counter := counter + Mux(io.clockConfig.provide8Mhz || io.doubleSpeed, 1.U, 2.U)
  }

  when (io.clockConfig.provide8Mhz || io.doubleSpeed) {
    when (io.doubleSpeed) {
      io.clocker.enable := io.clockConfig.enable
      io.clocker.pulse4Mhz := io.clocker.enable && counter(0)
    } .otherwise {
      io.clocker.enable := io.clockConfig.enable && counter(0)
      io.clocker.pulse4Mhz := io.clocker.enable
    }
  } .otherwise {
    io.clocker.enable := io.clockConfig.enable
    io.clocker.pulse4Mhz := io.clocker.enable
  }
  io.clocker.tCycle := Mux(io.doubleSpeed, counter(1, 0), counter(2, 1))
  io.clocker.phiPulse := io.clocker.enable && (io.clocker.tCycle === 3.U)
  io.clocker.pulseVramDma := io.clocker.enable && (counter(1, 0) === 3.U)
  io.clocker.counter8Mhz := counter(1, 0)
}