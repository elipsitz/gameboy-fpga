package gameboy.cart

import chisel3._
import chisel3.util._
import gameboy.CartridgeIo

object MbcType extends ChiselEnum {
  val None = Value
  val Mbc1 = Value
  val Mbc2 = Value
  val Mbc3 = Value
  val Mbc5 = Value
}

class EmuCartConfig extends Bundle {
  /** Whether the cartridge has a rumble feature (MBC5 and MBC7 only) */
  val hasRumble = Bool()
  /** Whether there is an RTC (MBC3 only) */
  val hasTimer = Bool()
  /** Whether there is any RAM (external, or for MBC2, internal)  */
  val hasRam = Bool()
  /** The MBC chip type */
  val mbcType = MbcType()
}

/**
 * An emulated cartridge.
 *
 * Provides the regular Gameboy cartridge interface.
 *   Assumes: (with some assumptions --
 *     At the first rising edge after tCycle 0 all of the memory access signals are ready
 *     The memory access signals don't change until after tCycle 3
 *
 * Gets the actual data through something with the EmuCartridgeDataAccess interface.
 */
class EmuCartridge extends Module {
  val io = IO(new Bundle {
    /** The current cart configuration (including MBC info) */
    val config = Input(new EmuCartConfig())
    /** Underlying data access interface */
    val dataAccess = new EmuCartridgeDataAccess()
    /** Emulated gameboy cartridge interface */
    val cartridgeIo = Flipped(new CartridgeIo())
    /** Gameboy t-cycle */
    val tCycle = Input(UInt(2.W))
    /** Whether we're waiting on the results of a data access */
    val waitingForAccess = Output(Bool())
  })

  // True if we're accessing ROM (rather than RAM)
  val selectRom = io.cartridgeIo.chipSelect
  // Whether there's an outstanding data access
  val waitingForAccess = RegInit(false.B)
  val accessEnable = Wire(Bool())

  when (accessEnable && io.tCycle === 1.U) {
    waitingForAccess := true.B
  }
  when (waitingForAccess && io.dataAccess.valid) {
    waitingForAccess := false.B
  }
  io.waitingForAccess := waitingForAccess
  io.dataAccess.enable := accessEnable && io.tCycle > 0.U

  val mbc = Module(new EmuMbc)
  mbc.io.config := io.config
  mbc.io.mbc.memEnable := io.cartridgeIo.enable
  mbc.io.mbc.memWrite := io.cartridgeIo.write && io.tCycle === 3.U
  mbc.io.mbc.memAddress := io.cartridgeIo.address
  mbc.io.mbc.memDataWrite := io.cartridgeIo.dataWrite
  mbc.io.mbc.selectRom := selectRom

  // We cannot write to ROM
  io.dataAccess.write := io.cartridgeIo.write && !selectRom
  io.dataAccess.selectRom := selectRom
  io.dataAccess.dataWrite := io.cartridgeIo.dataWrite
  when (selectRom) {
    accessEnable := io.cartridgeIo.enable
    io.dataAccess.write := false.B // We cannot write to ROM
    io.dataAccess.address := Cat(
      Mux(io.cartridgeIo.address(14), mbc.io.mbc.bankRom2, mbc.io.mbc.bankRom1),
      io.cartridgeIo.address(13, 0),
    )
    io.cartridgeIo.dataRead := io.dataAccess.dataRead
  } .otherwise {
    // Only do data access if RAM isn't mapped to the MBC registers, and if we actually have RAM
    // Don't do data access if we're accessing RAM and RAM is being mapped to the MBC
    //   OR if we're accessing RAM but there's no actual RAM
    accessEnable := io.cartridgeIo.enable && !mbc.io.mbc.ramReadMbc && io.config.hasRam
    io.dataAccess.write := io.cartridgeIo.write
    io.dataAccess.address := Cat(mbc.io.mbc.bankRam, io.cartridgeIo.address(12, 0))
    io.cartridgeIo.dataRead := Mux(mbc.io.mbc.ramReadMbc, mbc.io.mbc.memDataRead, io.dataAccess.dataRead)
  }
}


/**
 * Max ROM: 8 MiB (23 bits)
 * Max RAM: 128 KiB (17 bits)
 */
class EmuCartridgeDataAccess extends Bundle {
  val enable = Output(Bool())
  val write = Output(Bool())
  val selectRom = Output(Bool())
  val address = Output(UInt(23.W))
  val dataWrite = Output(UInt(8.W))
  val dataRead = Input(UInt(8.W))
  /** Whether the last access has completed */
  val valid = Input(Bool())
}