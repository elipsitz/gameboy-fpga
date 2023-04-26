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
  val regDacEnable = RegInit(VecInit(Seq.fill(4)(false.B)))
  val regChannelEnable = RegInit(VecInit(Seq.fill(4)(false.B)))
  val channelTrigger = WireDefault(VecInit(Seq.fill(4)(false.B)))
  val regLengthEnable = RegInit(VecInit(Seq.fill(4)(false.B)))
  // Channel 1
  val channel1Sweep = RegInit(0.U.asTypeOf(new RegisterPulseSweep))
  val channel1LengthInitial = RegInit(0.U(6.W))
  val channel1Duty = RegInit(0.U(2.W))
  val channel1Volume = RegInit(0.U.asTypeOf(new RegisterPulseVolume))
  val channel1Wavelength = RegInit(0.U(11.W))

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
        channel1LengthInitial := io.reg.dataWrite(5, 0)
      }
      is (0x12.U) { channel1Volume := io.reg.dataWrite.asTypeOf(new RegisterPulseVolume) }
      is (0x13.U) { channel1Wavelength := Cat(channel1Wavelength(10, 8), io.reg.dataWrite) }
      is (0x14.U) {
        channelTrigger(0) := true.B
        regLengthEnable(0) := io.reg.dataWrite(6)
        channel1Wavelength := Cat(io.reg.dataWrite(2, 0), channel1Wavelength)
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

  // TODO: if APU is disabled, zero out registers

  // Test tone, 440 Hz
  val period = (4 * 1024 * 1024) / 440
  val value = RegInit(false.B)
  val (counterValue, counterWrap) = Counter(true.B, period / 2)
  when (counterWrap) {
    value := !value
  }
  io.output.left := Mux(value, -480.S, 480.S)
  io.output.right := Mux(value, -480.S, 480.S)
}
