package platform

import axi.{AxiLiteInitiator, AxiLiteSignals, AxiLiteTarget}
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
    val axiInitiator = new AxiLiteSignals(32, 64)
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
  // TODO: support both physical cartridge and emulated
//  io.cartridge <> gameboy.io.cartridge
  io.cartridge.write := false.B
  io.cartridge.enable := false.B
  io.cartridge.dataWrite := 0.U
  io.cartridge.chipSelect := false.B
  io.cartridge.address := 0.U

  io.ppu <> gameboy.io.ppu
  io.joypad <> gameboy.io.joypad
  io.apu <> gameboy.io.apu
  io.tCycle <> gameboy.io.tCycle

  // Configuration registers
  val statRegStalls = RegInit(0.U(32.W))
  val configRegControl = RegInit(0.U.asTypeOf(new RegControl))
  val configRegRomAddress = RegInit(0.U(32.W))
  val axiTarget = Module(new AxiLiteTarget(Registers.maxId))
  io.axiTarget <> axiTarget.io.signals
  axiTarget.io.readData := VecInit(
    configRegControl.asUInt,
    configRegRomAddress,
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
    statRegStalls,
  )
  when (axiTarget.io.writeEnable) {
    switch (axiTarget.io.writeIndex) {
      is (Registers.Control.id.U) { configRegControl := axiTarget.io.writeData.asTypeOf(new RegControl) }
      is (Registers.RomAddress.id.U) { configRegRomAddress := axiTarget.io.writeData }
    }
  }

  // Gameboy clock (4 MHz)
  val waitingForCart = RegInit(false.B)
  gameboyClock := withClock(io.clock_8mhz) {
    val clock = RegInit(false.B)
    // Stall if we're on the second half of t=3 and we're still waiting for the emulated cartridge.
    val cartStall = waitingForCart && gameboy.io.tCycle === 3.U && !clock
    when (configRegControl.running) {
      when (cartStall) {
        statRegStalls := statRegStalls + 1.U
      } .otherwise {
        clock := !clock
      }
    }
    clock.asClock
  }
  io.clock_gameboy := gameboyClock

  // Emulated cartridge
  val axiInitiator = Module(new AxiLiteInitiator(32, 64))
  axiInitiator.io.signals <> io.axiInitiator
  val bufTCycle = RegNext(gameboy.io.tCycle)
  val bufTCycleLast = RegNext(RegNext(gameboy.io.tCycle))
  val bufCartEnable = RegNext(gameboy.io.cartridge.enable)
  val bufCartAddr = RegNext(gameboy.io.cartridge.address)
  // TODO write
  val readingAddress = configRegRomAddress + bufCartAddr
  axiInitiator.io.address := Cat(readingAddress(31, 3), 0.U(3.W))
  axiInitiator.io.read := true.B
  axiInitiator.io.enable := false.B
  val readingSubword = RegInit(0.U(3.W))
  when (bufTCycleLast === 0.U && bufTCycle === 1.U) {
    waitingForCart := bufCartEnable
    when (bufCartEnable) {
      axiInitiator.io.enable := true.B
      readingSubword := readingAddress(2, 0)
    }
  }
  when (waitingForCart && !axiInitiator.io.busy) {
    waitingForCart := false.B
  }
  gameboy.io.cartridge.dataRead := axiInitiator.io.readData.asTypeOf(Vec(8, UInt(8.W)))(readingSubword)
}