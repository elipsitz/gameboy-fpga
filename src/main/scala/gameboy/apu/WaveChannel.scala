package gameboy.apu

import chisel3._

/** Channel 3: reads samples from wave ram and plays them back */
class WaveChannel extends Module {
  val io = IO(new ChannelIO {
    val lengthConfig = Input(new LengthControlConfig(8))

    val dacEnable = Input(Bool())
    val volume = Input(UInt(2.W))
    val wavelength = Input(UInt(11.W))
  })

  // Length control module.
  val lengthUnit = Module(new LengthControl(8))
  lengthUnit.io.trigger := io.trigger
  lengthUnit.io.config := io.lengthConfig
  lengthUnit.io.tick := io.ticks.length

  // TODO: read this from wave ram
  val currentSample = 0.U(4.W)

  io.out := VecInit(0.U, currentSample, currentSample >> 1, currentSample >> 2)(io.volume)
  io.dacEnabled := io.dacEnable
  io.active := lengthUnit.io.channelEnable
}
