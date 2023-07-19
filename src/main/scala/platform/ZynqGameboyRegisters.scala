package platform

import chisel3._


object Registers extends Enumeration {
  /// Game Boy Control: index = 0
  val Control = Value(0)

  /// Emulated cartridge: index = 32
  val EmuCartConfig = Value(32)
  val RomAddress = Value(33)
  val RomMask = Value(34)
  val RamAddress = Value(35)
  val RamMask = Value(36)
  val RtcState = Value(37)
  val RtcStateLatched = Value(38)

  /// Framebuffer: index = 64
  val BlitControl = Value(64)
  val BlitAddress = Value(65)

  /// Debug: index = 96
  val CpuDebug1 = Value(96)
  val CpuDebug2 = Value(97)
  val CpuDebug3 = Value(98)
  val SerialDebug = Value(99)

  /// Stats: index = 128
  val StatCartStalls = Value(128)
  val StatNumClocks = Value(129)
  val StatCacheHits = Value(130)
  val StatCacheMisses = Value(131)
}

/** Gameboy Control */
class RegControl extends Bundle {
  // Bit 1 [R/W]: is gameboy in reset?
  val reset = Bool()
  // Bit 0 [R/W]: is gameboy running?
  val running = Bool()
}

/** CPU Debug 1 (Read-Only) */
class RegCpuDebug1 extends Bundle {
  val regB = UInt(8.W)
  val regC = UInt(8.W)
  val regD = UInt(8.W)
  val regE = UInt(8.W)
}

/** CPU Debug 2 (Read-Only) */
class RegCpuDebug2 extends Bundle {
  val regH = UInt(8.W)
  val regL = UInt(8.W)
  val regF = UInt(8.W)
  val regA = UInt(8.W)
}

/** CPU Debug 3 (Read-Only) */
class RegCpuDebug3 extends Bundle {
  val regSp = UInt(16.W)
  val regPc = UInt(16.W)
}

class RegBlitControl extends Bundle {
  // Bit 0 [R/W]: whether the blit operation should run
  val start = Bool()
}