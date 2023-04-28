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
  val regChannelEnable = RegInit(VecInit(Seq.fill(4)(false.B)))
  val regLengthEnable = RegInit(VecInit(Seq.fill(4)(false.B)))
  val channelTrigger = VecInit(Seq.fill(4)(false.B))
  // Channel 1
  val channel1Volume = RegInit(0.U.asTypeOf(new VolumeEnvelopeConfig))
  val channel1Sweep = RegInit(0.U.asTypeOf(new RegisterPulseSweep))
  val channel1Length = RegInit(0.U(6.W))
  val channel1Duty = RegInit(0.U(2.W))
  val channel1Wavelength = RegInit(0.U(11.W))

  // DAC
  val dacEnable = VecInit(
    channel1Volume.asUInt(7, 3) =/= 0.U,
    false.B, false.B, false.B)

  // Frame sequencer
  val frameSequencer = Module(new FrameSequencer)
  frameSequencer.io.divApu := io.divApu

  // Length
  when (frameSequencer.io.tick.length) {
    when(channel1Length =/= 0.U && regLengthEnable(0)) {
      val newLength = channel1Length - 1.U
      channel1Length := newLength
      when (newLength === 0.U) {
        regChannelEnable(0) := false.B
        printf(cf"chan1 len disable\n")
      }
    }
  }

  // Channel 1 frequency sweep
  val freqSweepShadow = RegInit(0.U(11.W))
  val freqSweepEnabled = RegInit(false.B)
  val freqSweepTimer = RegInit(0.U(3.W))
  when (frameSequencer.io.tick.frequency && freqSweepEnabled) {
    freqSweepTimer := freqSweepTimer - 1.U
    when (freqSweepTimer === 0.U) {
      freqSweepTimer := channel1Sweep.pace
      val offset = (freqSweepShadow >> channel1Sweep.slope).asUInt
      when (channel1Sweep.decrease) {
        freqSweepShadow := freqSweepShadow - offset
      } .otherwise {
        val newShadow = freqSweepShadow +& offset
        freqSweepShadow := newShadow(10, 0)
        when (newShadow >= 2048.U) {
          printf(cf"sweep overflow \n")
          regChannelEnable(0) := false.B
        }
      }
    }
  }


  // Channel modules
  val channel1VolumeUnit = Module(new VolumeEnvelope)
  channel1VolumeUnit.io.trigger := channelTrigger(0)
  channel1VolumeUnit.io.tick := frameSequencer.io.tick.volume
  channel1VolumeUnit.io.config := channel1Volume
  val channel1 = Module(new PulseChannel)
  channel1.io.wavelength := freqSweepShadow // channel1Wavelength
  channel1.io.duty := channel1Duty
  channel1.io.volume := channel1VolumeUnit.io.out
  val channel2 = Module(new SilentChannel)
  val channel3 = Module(new SilentChannel)
  val channel4 = Module(new SilentChannel)
  val channels: Seq[ChannelIO] = Seq(channel1.io, channel2.io, channel3.io, channel4.io)
  for (i <- 0 to 3) {
    channels(i).trigger := channelTrigger(i)
  }

  // Register memory mapping
  io.reg.valid := false.B
  io.reg.dataRead := DontCare
  when (io.reg.enabled && io.reg.write) {
    switch (io.reg.address) {
      is (0x26.U) { regApuEnable := io.reg.dataWrite(7) }
      is (0x25.U) { regPanning := io.reg.dataWrite.asTypeOf(new RegisterSoundPanning) }
      is (0x24.U) { regVolume := io.reg.dataWrite.asTypeOf(new RegisterMasterVolume) }
      is (0x10.U) { channel1Sweep := io.reg.dataWrite.asTypeOf(new RegisterPulseSweep) }
      is (0x11.U) {
        channel1Duty := io.reg.dataWrite(7, 6)
        channel1Length := ~io.reg.dataWrite(5, 0)
      }
      is (0x12.U) { channel1Volume := io.reg.dataWrite.asTypeOf(new VolumeEnvelopeConfig) }
      is (0x13.U) { channel1Wavelength := Cat(channel1Wavelength(10, 8), io.reg.dataWrite) }
      is (0x14.U) {
        regLengthEnable(0) := io.reg.dataWrite(6)
        val newWavelength = Cat(io.reg.dataWrite(2, 0), channel1Wavelength(7, 0))
        channel1Wavelength := newWavelength
        when (io.reg.dataWrite(7)) {
          // Trigger
          channelTrigger(0) := true.B
          regChannelEnable(0) := true.B
          when(channel1Length === 0.U) { channel1Length := 63.U }
          freqSweepShadow := newWavelength
          freqSweepTimer := channel1Sweep.pace
          freqSweepEnabled := (channel1Sweep.pace =/= 0.U) || (channel1Sweep.slope =/= 0.U)
        }
      }
    }
  }
  io.reg.dataRead := 0xFF.U
  when (io.reg.enabled && !io.reg.write) {
    switch (io.reg.address) {
      is (0x26.U) { io.reg.dataRead := Cat(regApuEnable, "b111".U(3.W), regChannelEnable.asUInt) }
      is (0x25.U) { io.reg.dataRead := regPanning.asUInt }
      is (0x24.U) { io.reg.dataRead := regVolume.asUInt }
      is (0x10.U) { io.reg.dataRead := channel1Sweep.asUInt }
      is (0x11.U) { io.reg.dataRead := Cat(channel1Duty, "b111111".U(6.W)) }
      is (0x12.U) { io.reg.dataRead := channel1Volume.asUInt }
      is (0x14.U) { io.reg.dataRead := Cat("b1".U(1.W), regLengthEnable(0), "b111111".U(6.W)) }
    }
  }
  io.reg.valid := io.reg.address >= 0x10.U && io.reg.address <= 0x3F.U

  // Mixer
  val dacOutput = VecInit((0 to 3).map(i =>
    Mux(dacEnable(i) && regChannelEnable(i), 0xF.S(5.W) - (channels(i).out << 1).asSInt, 0.S)
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
