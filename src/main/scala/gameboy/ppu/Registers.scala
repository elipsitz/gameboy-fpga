package gameboy.ppu

import chisel3._

/** FF40 -- LCDC: LCD control */
class RegisterLcdControl extends Bundle {
  /** LCD and PPU enable: 0=Off, 1=On */
  val enable = Bool()
  /** Window tile map area: 0=9800-9BFF, 1=9C00-9FFF */
  val windowTileMapArea = UInt(1.W)
  /** Window enable: 0=Off, 1=On */
  val windowEnable = Bool()
  /** BG and Window tile data area: 0=8800-97FF, 1=8000-8FFF */
  val bgTileDataArea = UInt(1.W)
  /** BG tile map area: 0=9800-9BFF, 1=9C00-9FFF */
  val bgTileMapArea = UInt(1.W)
  /** OBJ size: 0=8x8, 1=8x16 */
  val objSize = UInt(1.W)
  /** OBJ enable: 0=off, 1=On */
  val objEnable = Bool()
  /** BG and Window enable (DMG)/priority (CGB): 0=off, 1=on */
  val bgEnable = Bool()
}

/** FF41 -- STAT: LCD status */
class RegisterStatus extends Bundle {
  /** Enable LYC=LY STAT interrupt source */
  val interruptLyEnable = Bool()
  /** Enable Mode 2 OAM Search STAT interrupt source */
  val interruptOamEnable = Bool()
  /** Enable Mode 1 VBlank STAT interrupt source */
  val interruptVblankEnable = Bool()
  /** Enable Mode 0 HBlank STAT interrupt source */
  val interruptHblankEnable = Bool()

  // Bit 7: unimplemented, always 1
  // Bit 2: LYC=LY Flag
  // Bit 1-0: Current mode flag
}

/** BG, OBJ palette data register */
class RegisterPalette extends Bundle {
  /**
   * Bit 7-6 - Color for index 3
   * Bit 5-4 - Color for index 2
   * Bit 3-2 - Color for index 1
   * Bit 1-0 - Color for index 0
   */
  val colors = Vec(4, UInt(2.W))
}