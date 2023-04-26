package gameboy.apu

import chisel3._
import chisel3.util._

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
  })


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
