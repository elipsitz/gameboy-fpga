package gameboy.apu

import chisel3._

class NoiseChannelConfig extends Bundle {
  /** Clock shift "s" */
  val shift = UInt(4.W)
  /** LFSR width: 0 -> 15 bits, 1 -> 7 bits */
  val size = Bool()
  // Clock divider "r"
  val divider = UInt(3.W)
}

/** Channel 4: Uses LFSR to produce pseudorandom noise */
class NoiseChannel extends Module {
  val io = IO(new ChannelIO {
    val lengthConfig = Input(new LengthControlConfig(6))
    val volumeConfig = Input(new VolumeEnvelopeConfig)

  })

  // Length control module.
  val lengthUnit = Module(new LengthControl(8))
  lengthUnit.io.trigger := io.trigger
  lengthUnit.io.config := io.lengthConfig
  lengthUnit.io.tick := io.ticks.length

  // Volume control module.
  val volumeUnit = Module(new VolumeEnvelope)
  volumeUnit.io.trigger := io.trigger
  volumeUnit.io.config := io.volumeConfig
  volumeUnit.io.tick := io.ticks.volume

  io.out := 0.U // TODO
  io.dacEnabled := false.B // TODO
  io.active := lengthUnit.io.channelEnable
}
