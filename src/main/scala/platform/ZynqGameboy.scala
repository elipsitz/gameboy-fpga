package platform

import axi.{AxiLiteInitiator, AxiLiteSignals, AxiLiteTarget}
import chisel3._
import chisel3.util._
import gameboy.apu.ApuOutput
import gameboy.cart.{EmuCartConfig, EmuCartridge, Mbc3RtcAccess, RtcState}
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
    val framebufferWriteData = Output(UInt(15.W))

    // AXI Target (runs at the clock used to generate Gameboy clock)
    val axiTarget = Flipped(new AxiLiteSignals(log2Ceil(Registers.maxId * 4)))
    // AXI initiator for emulated cart, runs at "clock_emu_cart"
    val axiInitiator = new AxiLiteSignals(32, 64)
  })

  // Gameboy
  val gameboyConfig = Gameboy.Configuration(
    skipBootrom = false,
    optimizeForSimulation = false,
    model = Gameboy.Model.Dmg,
  )
  val gameboy = Module(new Gameboy(gameboyConfig))
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
  val configRegBlitAddress = RegInit(0.U(32.W))
  val configRegBlitControl = RegInit(0.U.asTypeOf(new RegBlitControl))
  val rtcAccess = Wire(new Mbc3RtcAccess)
  rtcAccess.writeEnable := false.B
  rtcAccess.writeState := DontCare
  rtcAccess.latchSelect := DontCare

  val axiTarget = Module(new AxiLiteTarget(Registers.maxId))
  io.axiTarget <> axiTarget.io.signals
  axiTarget.io.readData := 0.U
  switch (axiTarget.io.readIndex) {
    is (Registers.Control.id.U) { axiTarget.io.readData := configRegControl.asUInt }
    is (Registers.EmuCartConfig.id.U) { axiTarget.io.readData := configRegEmuCart.asUInt }
    is (Registers.RomAddress.id.U) { axiTarget.io.readData := configRegRomAddress }
    is (Registers.RomMask.id.U) { axiTarget.io.readData := configRegRomMask }
    is (Registers.RamAddress.id.U) { axiTarget.io.readData := configRegRamAddress }
    is (Registers.RamMask.id.U) { axiTarget.io.readData := configRegRamMask }
    is (Registers.CpuDebug1.id.U) {
      axiTarget.io.readData := (new RegCpuDebug1).Init(
        _.regB -> gameboy.io.cpuDebug.regB,
        _.regC -> gameboy.io.cpuDebug.regC,
        _.regD -> gameboy.io.cpuDebug.regD,
        _.regE -> gameboy.io.cpuDebug.regE,
      ).asUInt
    }
    is (Registers.CpuDebug2.id.U) {
      axiTarget.io.readData := (new RegCpuDebug2).Init(
        _.regH -> gameboy.io.cpuDebug.regH,
        _.regL -> gameboy.io.cpuDebug.regL,
        _.regF -> gameboy.io.cpuDebug.regF,
        _.regA -> gameboy.io.cpuDebug.regA,
      ).asUInt
    }
    is (Registers.CpuDebug3.id.U) {
      axiTarget.io.readData := (new RegCpuDebug3).Init(
        _.regSp -> gameboy.io.cpuDebug.regSp,
        _.regPc -> gameboy.io.cpuDebug.regPc,
      ).asUInt
    }
    is (Registers.StatCartStalls.id.U) { axiTarget.io.readData := statNumStalls }
    is (Registers.StatNumClocks.id.U) { axiTarget.io.readData := statNumClocks }
    is (Registers.BlitAddress.id.U) { axiTarget.io.readData := configRegBlitAddress }
    is (Registers.BlitControl.id.U) { axiTarget.io.readData := configRegBlitControl.asUInt }
    is (Registers.RtcState.id.U) {
      rtcAccess.latchSelect := false.B
      axiTarget.io.readData := rtcAccess.readState.asUInt
    }
    is (Registers.RtcStateLatched.id.U) {
      rtcAccess.latchSelect := true.B
      axiTarget.io.readData := rtcAccess.readState.asUInt
    }
  }
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
      is (Registers.BlitAddress.id.U) { configRegBlitAddress := axiTarget.io.writeData }
      is (Registers.BlitControl.id.U) {
        configRegBlitControl := axiTarget.io.writeData.asTypeOf(new RegBlitControl)
      }
      is (Registers.RtcState.id.U) {
        rtcAccess.writeEnable := true.B
        rtcAccess.latchSelect := false.B
        rtcAccess.writeState := axiTarget.io.writeData.asTypeOf(new RtcState)
      }
      is (Registers.RtcStateLatched.id.U) {
        rtcAccess.writeEnable := true.B
        rtcAccess.latchSelect := true.B
        rtcAccess.writeState := axiTarget.io.writeData.asTypeOf(new RtcState)
      }
    }
  }

  // Gameboy clock control
  val waitingForCart = Wire(Bool())
  // Stall if the access deadline is here but we're still waiting for the emulated cartridge.
  val cartStall = gameboy.io.cartridge.deadline && waitingForCart
  gameboy.io.clockConfig.enable := false.B
  when (configRegControl.running) {
    when (cartStall) {
      statNumStalls := statNumStalls + 1.U
    } .otherwise {
      gameboy.io.clockConfig.enable := true.B
      statNumClocks := statNumClocks + 1.U
    }
  }
  gameboy.io.clockConfig.provide8Mhz := true.B
  gameboy.reset := configRegControl.reset

  // Framebuffer output
  val framebufferX = RegInit(0.U(8.W))
  val framebufferY = RegInit(0.U(8.W))

  val prevHblank = RegInit(false.B)
  when (gameboy.io.clockConfig.enable) {
    prevHblank := gameboy.io.ppu.hblank
    when (gameboy.io.ppu.vblank) {
      framebufferX := 0.U
      framebufferY := 0.U
    } .elsewhen (gameboy.io.ppu.hblank && !prevHblank) {
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
  val emuCart = Module(new EmuCartridge(8 * 1024 * 1024))
  emuCart.io.config := configRegEmuCart
  emuCart.io.tCycle := gameboy.io.tCycle
  rtcAccess <> emuCart.io.rtcAccess
  when (configRegEmuCart.enabled) {
    waitingForCart := emuCart.io.waitingForAccess

    // Connect emulated cartridge
    emuCart.io.cartridgeIo <> gameboy.io.cartridge
    // Disconnect physical cartridge
    io.cartridge.write := false.B
    io.cartridge.enable := false.B
    io.cartridge.deadline := false.B
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
    emuCart.io.cartridgeIo.deadline := false.B
    emuCart.io.cartridgeIo.dataWrite := 0.U
    emuCart.io.cartridgeIo.chipSelect := false.B
    emuCart.io.cartridgeIo.address := 0.U
  }

  // Framebuffer blit
  val blitIndex = RegInit(0.U(15.W))
  val blitValidCount = RegInit(0.U(3.W))
  val blitMem = RegInit(0.U(64.W))
  val blitReadEnable = RegInit(false.B)
  val blitActive = configRegBlitControl.start
  val blitCurrent = blitMem.asTypeOf(Vec(4, UInt(16.W)))(blitIndex(1, 0))
  when (blitActive) {
    io.framebufferWriteAddr := blitIndex
    io.framebufferWriteData := blitCurrent(14, 0)
    when (blitValidCount === 0.U) {
      blitReadEnable := true.B
    } .otherwise {
      blitIndex := blitIndex + 1.U
      blitValidCount := blitValidCount - 1.U
      // Bit 15 is transparency -- 1 for visible, 0 for transparent
      io.framebufferWriteEnable := blitCurrent(15)

      when (blitIndex === ((160 * 144) - 1).U) {
        blitIndex := 0.U
        blitActive := false.B
        blitReadEnable := false.B
        blitValidCount := 0.U
      }
    }
  }

  // AXI Initiator for DRAM access -- runs ~100 MHz (integer multiple of module clock).
  val axiInitiatorReadData = Wire(UInt(64.W))
  val axiInitiatorReadValid = Wire(Bool())
  val axiInitiatorReadDataBuffer = RegNext(axiInitiatorReadData)
  val axiInitiatorReadValidBuffer = RegNext(axiInitiatorReadValid)
  val axiInitiator = withClock (io.clock_axi_dram) {
    val axiInitiator = Module(new AxiLiteInitiator(32, 64))
    axiInitiator.io.signals <> io.axiInitiator

    val bufferedDataEnable = RegNext(emuCart.io.dataAccess.enable)
    val bufferedDataEnableLast = RegNext(bufferedDataEnable)
    val bufferedDataAddr = RegNext(emuCart.io.dataAccess.address)
    val bufferedDataSelectRom = RegNext(emuCart.io.dataAccess.selectRom)
    val bufferedDataWriteData = RegNext(emuCart.io.dataAccess.dataWrite)
    val bufferedDataWrite = RegNext(emuCart.io.dataAccess.write)

    val bufferedBlitActive = RegNext(blitActive)
    val bufferedBlitReadEnable = RegNext(blitReadEnable)
    val bufferedBlitReadEnableLast = RegNext(bufferedBlitReadEnable)
    val bufferedBlitIndex = RegNext(blitIndex)

    when (bufferedBlitActive) {
      // Handle accessing blit data
      axiInitiator.io.address := configRegBlitAddress + (bufferedBlitIndex << 1.U)
      axiInitiator.io.enable := bufferedBlitReadEnable && !bufferedBlitReadEnableLast
      axiInitiator.io.read := true.B
      axiInitiator.io.writeData := DontCare
      axiInitiator.io.writeStrobe := DontCare
    } .otherwise {
      val accessAddress = Mux(
        bufferedDataSelectRom,
        configRegRomAddress + (bufferedDataAddr & configRegRomMask),
        configRegRamAddress + (bufferedDataAddr & configRegRamMask),
      )
      axiInitiator.io.address := Cat(accessAddress(31, 3), 0.U(3.W))
      axiInitiator.io.enable := bufferedDataEnable && !bufferedDataEnableLast
      axiInitiator.io.read := !bufferedDataWrite
      axiInitiator.io.writeData := Fill(8, bufferedDataWriteData)
      axiInitiator.io.writeStrobe := 1.U << accessAddress(2, 0)
    }

    axiInitiatorReadData := axiInitiator.io.readData
    axiInitiatorReadValid := !axiInitiator.io.busy

    axiInitiator
  }

  emuCart.io.dataAccess.dataRead := axiInitiatorReadDataBuffer
    .asTypeOf(Vec(8, UInt(8.W)))(
      emuCart.io.dataAccess.address(2, 0)
    )
  emuCart.io.dataAccess.valid := !blitActive && axiInitiatorReadValidBuffer

  when (blitActive && RegNext(blitReadEnable) && RegNext(blitReadEnable) && blitReadEnable && axiInitiatorReadValidBuffer) {
    blitReadEnable := false.B
    blitValidCount := 4.U
    blitMem := axiInitiatorReadDataBuffer
  }
}