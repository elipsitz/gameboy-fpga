package gameboy.apu

import chisel3._

/**
 * Base for channel 1 and channel 2.
 *
 * Internal counter (0..7) that selects either high or low from the duty cycle table.
 * Each step of this counter takes ((2048 - wavelength) * 4) cycles (full 4Mhz cycles).
 */
class PulseChannel extends Module {
  val io = IO(new ChannelIO {
    val lengthConfig = Input(new LengthControlConfig)
    val volumeConfig = Input(new VolumeEnvelopeConfig)

    val wavelength = Input(UInt(11.W))
    val duty = Input(UInt(2.W))
  })

  // Length control module.
  val lengthUnit = Module(new LengthControl(6))
  lengthUnit.io.trigger := io.trigger
  lengthUnit.io.config := io.lengthConfig
  lengthUnit.io.tick := io.ticks.length

  // Volume control module.
  val volumeUnit = Module(new VolumeEnvelope)
  volumeUnit.io.trigger := io.trigger
  volumeUnit.io.config := io.volumeConfig
  volumeUnit.io.tick := io.ticks.volume

  // Counter within the wave table. Only reset when APU turns off.
  val waveIndex = RegInit(0.U(3.W))
  // Counter that advances the waveIndex. Reset on trigger.
  val waveCounter = RegInit(0.U(14.W))
  // Wave counter counts up to this value.
  val waveCounterMax = (2048.U(14.W) - io.wavelength) << 2

  when (io.trigger) {
    waveCounter := waveCounterMax
  }

  when (waveCounter === 0.U) {
    waveIndex := waveIndex + 1.U
    waveCounter := waveCounterMax
  } .otherwise {
    waveCounter := waveCounter - 1.U
  }

  val dacEnable = io.volumeConfig.initialVolume =/= 0.U || io.volumeConfig.modeIncrease
  io.enabled := dacEnable && lengthUnit.io.channelEnable
  io.out := Mux(
    VecInit(waveIndex < 1.U, waveIndex < 2.U, waveIndex < 4.U, waveIndex < 6.U)(io.duty),
    volumeUnit.io.out, 0.U
  )
}

/** Channel 1: Pulse, but with additional frequency sweep **/
class PulseChannelWithSweep extends Module {
  val io = IO(new ChannelIO {

  })
}