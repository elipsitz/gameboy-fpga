package platform

import chisel3._

object Registers extends Enumeration {
  val Control = Value
  val EmuCartConfig = Value
  val RomAddress = Value
  val RomMask = Value
  val RamAddress = Value
  val RamMask = Value
  val CpuDebug1 = Value
  val CpuDebug2 = Value
  val CpuDebug3 = Value
  val StatCartStalls = Value
}

/** Register 0x0: Gameboy Control */
class RegControl extends Bundle {
  // Bit 0 [R/W]: is gameboy running?
  val running = Bool()
}

/** Register 0x1: CPU Debug 1 (Read-Only) */
class RegCpuDebug1 extends Bundle {
  val regB = UInt(8.W)
  val regC = UInt(8.W)
  val regD = UInt(8.W)
  val regE = UInt(8.W)
}

/** Register 0x2: CPU Debug 2 (Read-Only) */
class RegCpuDebug2 extends Bundle {
  val regH = UInt(8.W)
  val regL = UInt(8.W)
  val regF = UInt(8.W)
  val regA = UInt(8.W)
}

/** Register 0x3: CPU Debug 3 (Read-Only) */
class RegCpuDebug3 extends Bundle {
  val regSp = UInt(16.W)
  val regPc = UInt(16.W)
}