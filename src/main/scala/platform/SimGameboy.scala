package platform

import chisel3._
import chisel3.util._
import gameboy.apu.ApuOutput
import gameboy.cart.{CartConfig, EmuMbc}
import gameboy.{Gameboy, JoypadState}
import gameboy.ppu.PpuOutput

object SimGameboy extends App {
  emitVerilog(new SimGameboy, args)
}

class SimGameboy extends Module {
  val io = IO(new Bundle {
    val ppu = new PpuOutput
    val joypad = Input(new JoypadState)
    val apu = new ApuOutput

    // Emulated cartridge -- the interface to the simulator's memory buffer
    val cartConfig = Input(new CartConfig())
    val cartEnable = Output(Bool())
    val cartWrite = Output(Bool())
    val cartRomSelect = Output(Bool())
    /** The full address of the backing ROM/RAM of the cartridge */
    val cartAddress = Output(UInt(23.W))
    val cartDataWrite = Output(UInt(8.W))
    val cartDataRead = Input(UInt(8.W))
  })

  // Gameboy
  val gameboyConfig = Gameboy.Configuration(
    skipBootrom = true,
    optimizeForSimulation = true,
  )
  val gameboy = Module(new Gameboy(gameboyConfig))
  io.ppu <> gameboy.io.ppu
  io.joypad <> gameboy.io.joypad
  io.apu <> gameboy.io.apu

  // Emulated cartridge
  val mbc = Module(new EmuMbc)
  mbc.io.config := io.cartConfig
  mbc.io.mbc.memEnable := gameboy.io.cartridge.enable
  mbc.io.mbc.memWrite := gameboy.io.cartridge.write && gameboy.io.tCycle === 3.U
  mbc.io.mbc.memAddress := gameboy.io.cartridge.address
  mbc.io.mbc.memDataWrite := gameboy.io.cartridge.dataWrite
  // Disable if we're accessing RAM but RAM is being mapped to the MBC
  io.cartEnable := gameboy.io.cartridge.enable && (gameboy.io.cartridge.chipSelect || !mbc.io.mbc.ramReadMbc)
  // We can only *write* to RAM
  io.cartWrite := gameboy.io.cartridge.write && !gameboy.io.cartridge.chipSelect
  io.cartRomSelect := gameboy.io.cartridge.chipSelect
  io.cartDataWrite := gameboy.io.cartridge.dataWrite
  when (gameboy.io.cartridge.chipSelect) {
    // ROM
    io.cartAddress := Cat(
      Mux(gameboy.io.cartridge.address(14), mbc.io.mbc.bankRom2, mbc.io.mbc.bankRom1),
      gameboy.io.cartridge.address(13, 0),
    )
    gameboy.io.cartridge.dataRead := io.cartDataRead
  } .otherwise {
    // RAM
    io.cartAddress := Cat(mbc.io.mbc.bankRam, gameboy.io.cartridge.address(12, 0))
    gameboy.io.cartridge.dataRead := Mux(mbc.io.mbc.ramReadMbc, mbc.io.mbc.memDataRead, io.cartDataRead)
  }
  // TODO handle no RAM at all
}