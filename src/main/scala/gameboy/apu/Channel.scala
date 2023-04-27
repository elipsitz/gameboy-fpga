package gameboy.apu

import chisel3._
import chisel3.util.Counter

class ChannelIO extends Bundle {
  val out = Output(UInt(4.W))
  val trigger = Input(Bool())
}

/** Silent no-op channel for testing */
class SilentChannel extends Module {
  val io = IO(new ChannelIO())
  io.out := 0.U
}

/**
 * Channel 1 and 2: Pulse
 *
 * Internal counter (0..7) that selects either high or low from the duty cycle table.
 * Each step of this counter takes ((2048 - wavelength) * 4) cycles (full 4Mhz cycles).
 */
class PulseChannel extends Module {
  val io = IO(new ChannelIO {
    val wavelength = Input(UInt(11.W))
    val duty = Input(UInt(2.W))
  })

  // Counter within the wave table. Only reset when APU turns off.
  val waveIndex = RegInit(0.U(3.W))
  // Counter that advances the waveIndex. Reset on trigger.
  val waveCounter = RegInit(0.U(14.W))
  // Wave counter counts up to this value.
  val waveCounterMax = (2048.U(14.W) - io.wavelength) << 2

  when (io.trigger) {
    waveCounter := waveCounterMax
    printf(cf"trigger! wavelength=${io.wavelength} max=${waveCounterMax}\n");
  }

  when (waveCounter === 0.U) {
    waveIndex := waveIndex + 1.U
    waveCounter := waveCounterMax
  } .otherwise {
    waveCounter := waveCounter - 1.U
  }

  io.out := Mux(waveIndex < 4.U, 0xF.U, 0.U)
}