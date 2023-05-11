package platform

import axi.{AxiLiteSignals, AxiLiteTarget}
import chisel3._
import chisel3.util._
import gameboy.apu.ApuOutput
import gameboy.{CartridgeIo, Gameboy, JoypadState}
import gameboy.ppu.PpuOutput
import gameboy.util.BundleInit.AddInitConstruct

object ZynqGameboy extends App {
  emitVerilog(new ZynqGameboy, args)
}

class ZynqGameboy extends Module {
  val io = IO(new Bundle {
    /** 8 MHz clock for Gameboy */
    val clock_8mhz = Input(Clock())
    /** 4 MHz (from Gameboy), for PPU, cartridge output */
    val clock_gameboy = Output(Clock())

    // Gameboy output
    val cartridge = new CartridgeIo()
    val ppu = new PpuOutput
    val joypad = Input(new JoypadState)
    val apu = new ApuOutput
    val tCycle = Output(UInt(2.W))

    // Zynq communication
    val axiTarget = Flipped(new AxiLiteSignals(log2Ceil(Registers.maxId * 4)))
  })

  // Gameboy
  val gameboyConfig = Gameboy.Configuration(
    skipBootrom = false,
    optimizeForSimulation = false,
  )
  val gameboyClock = Wire(Clock())
  val gameboy = withClock(gameboyClock) {
    Module(new Gameboy(gameboyConfig))
  }
  io.cartridge <> gameboy.io.cartridge
  io.ppu <> gameboy.io.ppu
  io.joypad <> gameboy.io.joypad
  io.apu <> gameboy.io.apu
  io.tCycle <> gameboy.io.tCycle

  // Configuration registers
  val configRegControl = RegInit(0.U.asTypeOf(new RegControl))
  val axiTarget = Module(new AxiLiteTarget(Registers.maxId))
  io.axiTarget <> axiTarget.io.signals
  axiTarget.io.readData := VecInit(
    configRegControl.asUInt,
    (new RegCpuDebug1).Init(
      _.regB -> gameboy.io.cpuDebug.regB,
      _.regC -> gameboy.io.cpuDebug.regC,
      _.regD -> gameboy.io.cpuDebug.regD,
      _.regE -> gameboy.io.cpuDebug.regE,
    ).asUInt,
    (new RegCpuDebug2).Init(
      _.regH -> gameboy.io.cpuDebug.regH,
      _.regL -> gameboy.io.cpuDebug.regL,
      _.regF -> gameboy.io.cpuDebug.regF,
      _.regA -> gameboy.io.cpuDebug.regA,
    ).asUInt,
    (new RegCpuDebug3).Init(
      _.regSp -> gameboy.io.cpuDebug.regSp,
      _.regPc -> gameboy.io.cpuDebug.regPc,
    ).asUInt,
  )
  when (axiTarget.io.writeEnable) {
    switch (axiTarget.io.writeIndex) {
      is (Registers.Control.id.U) { configRegControl := axiTarget.io.writeData.asTypeOf(new RegControl) }
    }
  }

  // Gameboy clock (4 MHz)
  gameboyClock := withClock(io.clock_8mhz) {
    val clock = RegInit(false.B)
    when (configRegControl.running) {
      clock := !clock
    }
    clock.asClock
  }
  io.clock_gameboy := gameboyClock
}