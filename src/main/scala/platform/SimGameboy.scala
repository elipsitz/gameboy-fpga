package platform

import chisel3._
import gameboy.apu.ApuOutput
import gameboy.{CartridgeIo, Gameboy, JoypadState}
import gameboy.ppu.PpuOutput

object SimGameboy extends App {
  emitVerilog(new SimGameboy, args)
}

class SimGameboy extends Module {
  val io = IO(new Bundle {
    // Gameboy output
    val cartridge = new CartridgeIo()
    val ppu = new PpuOutput
    val joypad = Input(new JoypadState)
    val apu = new ApuOutput
  })

  // Gameboy
  val gameboyConfig = Gameboy.Configuration(
    skipBootrom = true,
    optimizeForSimulation = true,
  )
  val gameboy = Module(new Gameboy(gameboyConfig))
  io.cartridge <> gameboy.io.cartridge
  io.ppu <> gameboy.io.ppu
  io.joypad <> gameboy.io.joypad
  io.apu <> gameboy.io.apu
}