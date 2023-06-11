package platform

import axi.{AxiLiteInitiator, AxiLiteSignals, AxiLiteTarget}
import chisel3._
import chisel3.util._
import gameboy.apu.ApuOutput
import gameboy.cart.{EmuCartConfig, EmuCartridge}
import gameboy.{CartridgeIo, Gameboy, JoypadState, SerialIo}
import gameboy.ppu.PpuOutput
import gameboy.util.BundleInit.AddInitConstruct

object ZynqGameboy extends App {
  emitVerilog(new ZynqGameboy, args)
}

/**
 * Clocked by the 8.3886 MHz "Gameboy" clock.
 */
class ZynqGameboy extends Module {
  val io = IO(new Bundle {
    /** ~100MHz clock, integer multiple of the Module clock, used for emulated cart AXI initiator */
    val clock_axi_dram = Input(Clock())

    // Gameboy output
    val cartridge = new CartridgeIo()
    val ppu = new PpuOutput
    val joypad = Input(new JoypadState)
    val apu = new ApuOutput
    val serial = new SerialIo()
    val tCycle = Output(UInt(2.W))

    // Framebuffer output
    val framebufferWriteAddr = Output(UInt(15.W))
    val framebufferWriteEnable = Output(Bool())
    val framebufferWriteData = Output(UInt(2.W))

    // AXI Target (runs at the clock used to generate Gameboy clock)
    val axiTarget = Flipped(new AxiLiteSignals(log2Ceil(Registers.maxId * 4)))
    // AXI initiator for emulated cart, runs at "clock_emu_cart"
    val axiInitiator = new AxiLiteSignals(32, 64)
  })

  // Gameboy
  val gameboyConfig = Gameboy.Configuration(
    skipBootrom = false,
    optimizeForSimulation = false,
  )
  val gameboyClock = RegInit(false.B)
  val gameboy = withClock(gameboyClock.asClock) {
    Module(new Gameboy(gameboyConfig))
  }

  io.ppu <> gameboy.io.ppu
  io.joypad <> gameboy.io.joypad
  io.apu <> gameboy.io.apu
  io.serial <> gameboy.io.serial
  io.tCycle <> gameboy.io.tCycle

  io.framebufferWriteAddr := DontCare
  io.framebufferWriteData := DontCare
  io.framebufferWriteEnable := false.B

  val statNumStalls = RegInit(0.U(32.W))
  val statNumClocks = RegInit(0.U(32.W))

  // Configuration registers
  val configRegControl = RegInit(0.U.asTypeOf(new RegControl))
  val configRegEmuCart = RegInit(0.U.asTypeOf(new EmuCartConfig))
  val configRegRomAddress = RegInit(0.U(32.W))
  val configRegRomMask = RegInit(0.U(23.W))
  val configRegRamAddress = RegInit(0.U(32.W))
  val configRegRamMask = RegInit(0.U(17.W))
  val axiTarget = Module(new AxiLiteTarget(Registers.maxId))
  io.axiTarget <> axiTarget.io.signals
  axiTarget.io.readData := VecInit(
    configRegControl.asUInt,
    configRegEmuCart.asUInt,
    configRegRomAddress,
    configRegRomMask,
    configRegRamAddress,
    configRegRamMask,
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
    statNumStalls,
    statNumClocks,
    0.U,
  )
  when (axiTarget.io.writeEnable) {
    switch (axiTarget.io.writeIndex) {
      is (Registers.Control.id.U) { configRegControl := axiTarget.io.writeData.asTypeOf(new RegControl) }
      is (Registers.EmuCartConfig.id.U) {
        suppressEnumCastWarning {
          configRegEmuCart := axiTarget.io.writeData.asTypeOf(new EmuCartConfig)
        }
      }
      is (Registers.RomAddress.id.U) { configRegRomAddress := axiTarget.io.writeData }
      is (Registers.RomMask.id.U) { configRegRomMask := axiTarget.io.writeData }
      is (Registers.RamAddress.id.U) { configRegRamAddress := axiTarget.io.writeData }
      is (Registers.RamMask.id.U) { configRegRamMask := axiTarget.io.writeData }
      is (Registers.Framebuffer.id.U) {
        io.framebufferWriteAddr := axiTarget.io.writeData(31, 16)
        io.framebufferWriteData := axiTarget.io.writeData(1, 0)
        io.framebufferWriteEnable := true.B
      }
    }
  }

  // Gameboy clock (4 MHz)
  val waitingForCart = Wire(Bool())
  // Stall if we're on the second half of t=3 and we're still waiting for the emulated cartridge.
  val cartStall = waitingForCart && gameboy.io.tCycle === 3.U && !gameboyClock
  when (configRegControl.running) {
    when (cartStall) {
      statNumStalls := statNumStalls + 1.U
    } .otherwise {
      gameboyClock := !gameboyClock
      statNumClocks := statNumClocks + 1.U
    }
  }
  gameboy.reset := configRegControl.reset
  when (configRegControl.reset) {
    gameboyClock := !gameboyClock
  }

  // Framebuffer output
  val framebufferX = RegInit(0.U(8.W))
  val framebufferY = RegInit(0.U(8.W))

  when (gameboyClock && !RegNext(gameboyClock)) {
    when (gameboy.io.ppu.vblank) {
      framebufferX := 0.U
      framebufferY := 0.U
    } .elsewhen (gameboy.io.ppu.hblank && !RegNext(gameboy.io.ppu.hblank)) {
      framebufferX := 0.U
      framebufferY := framebufferY + 1.U
    } .elsewhen (gameboy.io.ppu.valid) {
      io.framebufferWriteEnable := true.B
      io.framebufferWriteAddr := (framebufferY * 160.U(8.W)) + framebufferX
      io.framebufferWriteData := gameboy.io.ppu.pixel
      framebufferX := framebufferX + 1.U
    }
  }

  // Emulated cartridge and physical connections
  val emuCart = Module(new EmuCartridge())
  emuCart.io.config := configRegEmuCart
  emuCart.io.tCycle := gameboy.io.tCycle
  when (configRegEmuCart.enabled) {
    waitingForCart := emuCart.io.waitingForAccess

    // Connect emulated cartridge
    emuCart.io.cartridgeIo <> gameboy.io.cartridge
    // Disconnect physical cartridge
    io.cartridge.write := false.B
    io.cartridge.enable := false.B
    io.cartridge.dataWrite := 0.U
    io.cartridge.chipSelect := false.B
    io.cartridge.address := 0.U
  }.otherwise {
    waitingForCart := false.B

    // Connect physical cartridge
    io.cartridge <> gameboy.io.cartridge
    // Disconnect emulated cartridge
    emuCart.io.cartridgeIo.write := false.B
    emuCart.io.cartridgeIo.enable := false.B
    emuCart.io.cartridgeIo.dataWrite := 0.U
    emuCart.io.cartridgeIo.chipSelect := false.B
    emuCart.io.cartridgeIo.address := 0.U
  }

  // AXI Initiator for DRAM access -- runs ~100 MHz (integer multiple of module clock).
  val axiInitiatorReadData = Wire(UInt(8.W))
  val axiInitiatorReadValid = Wire(UInt(8.W))
  val axiInitiator = withClock (io.clock_axi_dram) {
    val axiInitiator = Module(new AxiLiteInitiator(32, 64))
    axiInitiator.io.signals <> io.axiInitiator

    val bufferedDataEnable = RegNext(emuCart.io.dataAccess.enable)
    val bufferedDataEnableLast = RegNext(bufferedDataEnable)
    val bufferedDataAddr = RegNext(emuCart.io.dataAccess.address)
    val bufferedDataSelectRom = RegNext(emuCart.io.dataAccess.selectRom)
    val bufferedDataWriteData = RegNext(emuCart.io.dataAccess.dataWrite)
    val bufferedDataWrite = RegNext(emuCart.io.dataAccess.write)

    val accessAddress = Mux(
      bufferedDataSelectRom,
      configRegRomAddress + (bufferedDataAddr & configRegRomMask),
      configRegRamAddress + (bufferedDataAddr & configRegRamMask),
    )
    val accessSubword = RegInit(0.U(3.W))
    axiInitiator.io.address := Cat(accessAddress(31, 3), 0.U(3.W))
    axiInitiator.io.enable := false.B
    axiInitiator.io.read := !bufferedDataWrite
    axiInitiator.io.writeData := Fill(8, bufferedDataWriteData)
    axiInitiator.io.writeStrobe := 1.U << accessAddress(2, 0)
    when(bufferedDataEnable && !bufferedDataEnableLast) {
      axiInitiator.io.enable := true.B
      accessSubword := accessAddress(2, 0)
    }

    axiInitiatorReadData := axiInitiator.io.readData.asTypeOf(Vec(8, UInt(8.W)))(accessSubword)
    axiInitiatorReadValid := !axiInitiator.io.busy

    axiInitiator
  }

  emuCart.io.dataAccess.dataRead := RegNext(axiInitiatorReadData)
  emuCart.io.dataAccess.valid := RegNext(axiInitiatorReadValid)
}