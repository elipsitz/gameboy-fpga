package gameboy.cart

import chisel3._

class MbcIo extends Bundle {
  /** Memory accesses */
  val memEnable = Input(Bool())
  val memWrite = Input(Bool())
  val memAddress = Input(UInt(16.W))
  val memDataWrite = Input(UInt(8.W))
  val memDataRead = Output(UInt(8.W))
  val selectRom = Input(Bool())

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

  val mbcNone = Module(new MbcNone())
  val mbc1 = Module(new Mbc1())
  val mbc3 = Module(new Mbc3())
  mbc3.io.hasRtc := io.config.hasRtc
  val mbc5 = Module(new Mbc5())

  val mbcs = Seq(
    MbcType.None -> mbcNone.io,
    MbcType.Mbc1 -> mbc1.io,
    MbcType.Mbc3 -> mbc3.io,
    MbcType.Mbc5 -> mbc5.io,
  )

  io.mbc.memDataRead := DontCare
  io.mbc.bankRom1 := DontCare
  io.mbc.bankRom2 := DontCare
  io.mbc.bankRam := DontCare
  io.mbc.ramReadMbc := DontCare
  for ((id, mbc) <- mbcs) {
    mbc.memEnable := io.mbc.memEnable
    mbc.memWrite := io.mbc.memWrite
    mbc.memAddress := io.mbc.memAddress
    mbc.memDataWrite := io.mbc.memDataWrite
    mbc.selectRom := io.mbc.selectRom

    when (id === io.config.mbcType) {
      io.mbc.memDataRead := mbc.memDataRead
      io.mbc.bankRom1 := mbc.bankRom1
      io.mbc.bankRom2 := mbc.bankRom2
      io.mbc.bankRam := mbc.bankRam
      io.mbc.ramReadMbc := mbc.ramReadMbc
    }
  }
}
