package gameboy.apu

import chisel3._
import chisel3.util._
import gameboy.PeripheralAccess

class ApuOutput extends Bundle {
  /** Left sample value */
  val left = Output(SInt(10.W))
  /** Right sample value */
  val right = Output(SInt(10.W))
}

/**
 * Audio Processing Unit
 *
 * Has 4 channels, each of which outputs a sample in range 0..15, corresponding to 1.0 and -1.0.
 * These get added together (if an individual channel's DAC is enabled), for a range of -4.0 to 4.0.
 * Then, each left/right channel has a volume scaler, from 1x to 8x.
 *
 * To convert an unsigned channel sample (0..15) to a signed one, we do (0xF - (2 * value)), making a range of
 * -15 to 15. With four channels, -60 to 60. With the volume scaler, -480 to 480. This is a 10-bit signed integer.
 */
class Apu extends Module {
  val io = IO(new Bundle {
    val output = new ApuOutput
    val reg = new PeripheralAccess
    val divApu = Input(Bool())
  })

  // General registers
  val regApuEnable = RegInit(false.B)
  val regPanning = RegInit(0.U.asTypeOf(new RegisterSoundPanning))
  val regVolume = RegInit(0.U.asTypeOf(new RegisterMasterVolume))
  val regLengthEnable = RegInit(VecInit(Seq.fill(4)(false.B)))
  val channelTrigger = VecInit(Seq.fill(4)(false.B))

  // Wave ram
  val waveRam = RegInit(VecInit(Seq.fill(16)(0.U(8.W))))

  // Frame sequencer
  val frameSequencer = Module(new FrameSequencer)
  frameSequencer.io.divApu := io.divApu

  // Channel 1
  val channel1VolumeConfig = RegInit(0.U.asTypeOf(new VolumeEnvelopeConfig))
  val channel1SweepConfig = RegInit(0.U.asTypeOf(new FrequencySweepConfig))
  val channel1Duty = RegInit(0.U(2.W))
  val channel1Wavelength = RegInit(0.U(11.W))
  val channel1 = Module(new PulseChannelWithSweep)
  channel1.io.lengthConfig.length := DontCare
  channel1.io.lengthConfig.lengthLoad := false.B
  channel1.io.lengthConfig.enabled := regLengthEnable(0)
  channel1.io.volumeConfig := channel1VolumeConfig
  channel1.io.wavelength := channel1Wavelength
  channel1.io.duty := channel1Duty
  channel1.io.sweepConfig := channel1SweepConfig

  // Channel 2
  val channel2VolumeConfig = RegInit(0.U.asTypeOf(new VolumeEnvelopeConfig))
  val channel2Duty = RegInit(0.U(2.W))
  val channel2Wavelength = RegInit(0.U(11.W))
  val channel2 = Module(new PulseChannel)
  channel2.io.lengthConfig.length := DontCare
  channel2.io.lengthConfig.lengthLoad := false.B
  channel2.io.lengthConfig.enabled := regLengthEnable(1)
  channel2.io.volumeConfig := channel2VolumeConfig
  channel2.io.wavelength := channel2Wavelength
  channel2.io.duty := channel2Duty

  // Channel 3
  val channel3DacEnable = RegInit(false.B)
  val channel3Volume = RegInit(0.U(2.W))
  val channel3Wavelength = RegInit(0.U(11.W))
  val channel3 = Module(new WaveChannel)
  channel3.io.lengthConfig.length := DontCare
  channel3.io.lengthConfig.lengthLoad := false.B
  channel3.io.lengthConfig.enabled := regLengthEnable(3)
  channel3.io.dacEnable := channel3DacEnable
  channel3.io.volume := channel3Volume
  channel3.io.wavelength := channel3Wavelength

  // Channel 4
  val channel4 = Module(new SilentChannel)

  // Shared channel stuff
  val channels: Seq[ChannelIO] = Seq(channel1.io, channel2.io, channel3.io, channel4.io)
  for (i <- 0 to 3) {
    channels(i).trigger := channelTrigger(i)
    channels(i).ticks := frameSequencer.io.ticks
  }

  // Register memory mapping
  io.reg.valid := false.B
  io.reg.dataRead := DontCare
  when (io.reg.enabled && io.reg.write) {
    switch (io.reg.address) {
      // Global registers
      is (0x26.U) { regApuEnable := io.reg.dataWrite(7) }
      is (0x25.U) { regPanning := io.reg.dataWrite.asTypeOf(new RegisterSoundPanning) }
      is (0x24.U) { regVolume := io.reg.dataWrite.asTypeOf(new RegisterMasterVolume) }

      // Channel 1 Registers
      is (0x10.U) { channel1SweepConfig := io.reg.dataWrite.asTypeOf(new FrequencySweepConfig) }
      is (0x11.U) {
        channel1Duty := io.reg.dataWrite(7, 6)
        channel1.io.lengthConfig.length := io.reg.dataWrite(5, 0)
        channel1.io.lengthConfig.lengthLoad := true.B
      }
      is (0x12.U) { channel1VolumeConfig := io.reg.dataWrite.asTypeOf(new VolumeEnvelopeConfig) }
      is (0x13.U) { channel1Wavelength := Cat(channel1Wavelength(10, 8), io.reg.dataWrite) }
      is (0x14.U) {
        regLengthEnable(0) := io.reg.dataWrite(6)
        val newWavelength = Cat(io.reg.dataWrite(2, 0), channel1Wavelength(7, 0))
        channel1Wavelength := newWavelength
        channel1.io.wavelength := newWavelength
        when (io.reg.dataWrite(7)) { channelTrigger(0) := true.B }
      }

      // Channel 2 Registers
      is (0x16.U) {
        channel2Duty := io.reg.dataWrite(7, 6)
        channel2.io.lengthConfig.length := io.reg.dataWrite(5, 0)
        channel2.io.lengthConfig.lengthLoad := true.B
      }
      is (0x17.U) { channel2VolumeConfig := io.reg.dataWrite.asTypeOf(new VolumeEnvelopeConfig) }
      is (0x18.U) { channel2Wavelength := Cat(channel2Wavelength(10, 8), io.reg.dataWrite) }
      is (0x19.U) {
        regLengthEnable(1) := io.reg.dataWrite(6)
        val newWavelength = Cat(io.reg.dataWrite(2, 0), channel2Wavelength(7, 0))
        channel2Wavelength := newWavelength
        channel2.io.wavelength := newWavelength
        when (io.reg.dataWrite(7)) { channelTrigger(1) := true.B }
      }

      // Channel 3 Registers
      is (0x1A.U) { channel3DacEnable := io.reg.dataWrite(7) }
      is (0x1B.U) {
        channel3.io.lengthConfig.length := io.reg.dataWrite
        channel3.io.lengthConfig.lengthLoad := true.B
      }
      is (0x1C.U) { channel3.io.volume := io.reg.dataWrite(6, 5) }
      is (0x1D.U) { channel3Wavelength := Cat(channel3Wavelength(10, 8), io.reg.dataWrite) }
      is (0x1E.U) {
        regLengthEnable(2) := io.reg.dataWrite(6)
        val newWavelength = Cat(io.reg.dataWrite(2, 0), channel3Wavelength(7, 0))
        channel3Wavelength := newWavelength
        channel3.io.wavelength := newWavelength
        when (io.reg.dataWrite(7)) {
          channelTrigger(2) := true.B
        }
      }
    }

    // Wave RAM -- TODO, handle conflicts?
    when (io.reg.address >= 0x30.U && io.reg.address < 0x40.U) {
      waveRam(io.reg.address(3, 0)) := io.reg.dataWrite
    }
  }
  io.reg.dataRead := 0xFF.U
  when (io.reg.enabled && !io.reg.write) {
    switch (io.reg.address) {
      // Global registers
      is (0x26.U) {
        val channelsActive = VecInit(channels.reverse.map(c => c.active))
        io.reg.dataRead := Cat(regApuEnable, "b111".U(3.W), channelsActive.asUInt)
      }
      is (0x25.U) { io.reg.dataRead := regPanning.asUInt }
      is (0x24.U) { io.reg.dataRead := regVolume.asUInt }

      // Channel 1 registers
      is (0x10.U) { io.reg.dataRead := channel1SweepConfig.asUInt }
      is (0x11.U) { io.reg.dataRead := Cat(channel1Duty, "b111111".U(6.W)) }
      is (0x12.U) { io.reg.dataRead := channel1VolumeConfig.asUInt }
      is (0x14.U) { io.reg.dataRead := Cat("b1".U(1.W), regLengthEnable(0), "b111111".U(6.W)) }

      // Channel 2 registers
      is (0x16.U) { io.reg.dataRead := Cat(channel1Duty, "b111111".U(6.W)) }
      is (0x17.U) { io.reg.dataRead := channel1VolumeConfig.asUInt }
      is (0x19.U) { io.reg.dataRead := Cat("b1".U(1.W), regLengthEnable(1), "b111111".U(6.W)) }

      // Channel 3 Registers
      is (0x1A.U) { io.reg.dataRead := Cat(channel3DacEnable, "b1111111".U(7.W)) }
      is (0x1C.U) { io.reg.dataRead := Cat("b1".U(1.W), channel3.io.volume, "b11111".U(5.W)) }
      is (0x1E.U) { io.reg.dataRead := Cat("b1".U(1.W), regLengthEnable(2), "b111111".U(6.W)) }
    }

    when(io.reg.address >= 0x30.U && io.reg.address < 0x40.U) {
      io.reg.dataRead := waveRam(io.reg.address(3, 0))
    }
  }
  io.reg.valid := io.reg.address >= 0x10.U && io.reg.address <= 0x3F.U

  // Mixer
  val dacOutput = VecInit((0 to 3).map(i =>
    Mux(channels(i).active && channels(i).dacEnabled, 0xF.S(5.W) - (channels(i).out << 1).asSInt, 0.S)
  ))
  val mixerLeft = VecInit((0 to 3).map(i => Mux(regPanning.left(i), dacOutput(i), 0.S))).reduceTree(_ +& _)
  val mixerRight = VecInit((0 to 3).map(i => Mux(regPanning.right(i), dacOutput(i), 0.S))).reduceTree(_ +& _)
  io.output.left := mixerLeft * (regVolume.leftVolume +& 1.U).zext
  io.output.right := mixerRight * (regVolume.rightVolume +& 1.U).zext

  // val test = RegInit(0.U(7.W))
  // test := test + 1.U
  // when (test === 0.U) {
  //   printf(cf"chan=${channels(0).out} dac=${dacOutput(0)}  mixer = ${mixerLeft}, out = ${io.output.left}\n")
  // }
}
