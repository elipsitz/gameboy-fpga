package gameboy.cart

import chisel3._



class MbcIo extends Bundle {
  /** Memory accesses */
  val memEnable = Input(Bool())
  val memWrite = Input(Bool())
  val memAddress = Input(UInt(16.W))
  val memDataWrite = Input(UInt(8.W))
  val memDataRead = Output(UInt(8.W))

  /** Bank index for ROM region 0x0000-0x3FFF */
  val bankRom1 = Output(UInt(9.W))
  /** Bank index for ROM region 0x4000-0x7FFF */
  val bankRom2 = Output(UInt(9.W))
  /** Bank index for RAM region 0xA000-0xBFFF */
  val bankRam = Output(UInt(4.W))
  /** If true, use the MBC's memDataRead (instead of reading from main RAM) */
  val ramReadMbc = Output(Bool())
}

/**
 * An emulated memory bank controller (MBC)
 *
 * Controls bank switching. Observes writes to ROM and RAM regions
 * (potentially changing internal state). Can provide alternate data
 * for external RAM reads.
 *
 * Max ROM: 8 MiB (23 bits)
 * Max RAM: 128 KiB (17 bits)
 * Each ROM bank switches 16 KiB (14 bits)
 * Each RAM bank switches 8 KiB (13 bits)
 */
class EmuMbc extends Module {
  val io = IO(new Bundle {
    val config = Input(new EmuCartConfig())
    val mbc = new MbcIo()
  })

  val mbcs = Seq(
    MbcType.None -> Module(new MbcNone()),
    MbcType.Mbc1 -> Module(new MbcNone()), // TEST: TODO FIX
  )

  io.mbc.memDataRead := DontCare
  io.mbc.bankRom1 := DontCare
  io.mbc.bankRom2 := DontCare
  io.mbc.bankRam := DontCare
  io.mbc.ramReadMbc := DontCare
  for ((id, mbc) <- mbcs) {
    mbc.io.memEnable := io.mbc.memEnable
    mbc.io.memWrite := io.mbc.memWrite
    mbc.io.memAddress := io.mbc.memAddress
    mbc.io.memDataWrite := io.mbc.memDataWrite

    when (id === io.config.mbcType) {
      io.mbc.memDataRead := mbc.io.memDataRead
      io.mbc.bankRom1 := mbc.io.bankRom1
      io.mbc.bankRom2 := mbc.io.bankRom2
      io.mbc.bankRam := mbc.io.bankRam
      io.mbc.ramReadMbc := mbc.io.ramReadMbc
    }
  }
}
