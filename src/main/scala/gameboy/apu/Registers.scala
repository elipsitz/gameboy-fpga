package gameboy.apu

import chisel3._

/** FF25 — NR51: Sound panning */
class RegisterSoundPanning extends Bundle {
  val left = Vec(4, Bool())
  val right = Vec(4, Bool())
}

/** FF24 — NR50: Master volume & VIN panning */
class RegisterMasterVolume extends Bundle {
  val leftVin = Bool()
  val leftVolume = UInt(3.W)
  val rightVin = Bool()
  val rightVolume = UInt(3.W)
}
