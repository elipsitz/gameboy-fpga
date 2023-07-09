package platform

import chisel3._
import gameboy.apu.ApuOutput
import gameboy.cart.{EmuCartConfig, EmuCartridge, EmuCartridgeDataAccess, EmuMbc}
import gameboy.{Gameboy, JoypadState}
import gameboy.ppu.PpuOutput

object SimGameboy extends App {
  emitVerilog(new SimGameboy, args)
}

class SimGameboy extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())

    val ppu = new PpuOutput
    val joypad = Input(new JoypadState)
    val apu = new ApuOutput

    // Emulated cartridge data access interface
    val cartConfig = Input(new EmuCartConfig())
    val dataAccess = new EmuCartridgeDataAccess()
  })

  // Gameboy
  val gameboyConfig = Gameboy.Configuration(
    skipBootrom = true,
    optimizeForSimulation = true,
    model = Gameboy.Model.Cgb,
  )
  val gameboy = Module(new Gameboy(gameboyConfig))
  gameboy.io.enable := io.enable
  io.ppu <> gameboy.io.ppu
  io.joypad <> gameboy.io.joypad
  io.apu <> gameboy.io.apu

  // Disconnected serial
  gameboy.io.serial.clockIn := true.B
  gameboy.io.serial.in := true.B

  // Emulated cartridge
  val emuCart = Module(new EmuCartridge(4 * 1024 * 1024))
  gameboy.io.cartridge <> emuCart.io.cartridgeIo
  emuCart.io.dataAccess <> io.dataAccess
  emuCart.io.config := io.cartConfig
  emuCart.io.tCycle := gameboy.io.tCycle
  emuCart.io.rtcAccess.writeEnable := false.B
  emuCart.io.rtcAccess.writeState := DontCare
  emuCart.io.rtcAccess.latchSelect := DontCare
}