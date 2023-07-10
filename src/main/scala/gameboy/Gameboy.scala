package gameboy

import chisel3._
import chisel3.util._
import gameboy.apu.{Apu, ApuOutput}
import gameboy.cpu.Cpu
import gameboy.ppu.{Ppu, PpuOutput}
import gameboy.util.SinglePortRam

class CartridgeIo extends Bundle {
  /** Cartridge address selection */
  val address = Output(UInt(16.W))
  /** Whether a memory access is being performed */
  val enable = Output(Bool())
  /**
   * Whether the cartridge access started previously must complete by the next clock rise.
   * If the access isn't ready, the Gameboy should be stalled until it is.
   */
  val deadline = Output(Bool())
  /** Whether the memory access is a write */
  val write = Output(Bool())
  /** Chip select (high for ROM, low for RAM) */
  val chipSelect = Output(Bool())
  /** Data in (reading) */
  val dataRead = Input(UInt(8.W))
  /** Data out (writing) */
  val dataWrite = Output(UInt(8.W))
}

class JoypadState extends Bundle {
  val start = Bool()
  val select = Bool()
  val b = Bool()
  val a = Bool()
  val down = Bool()
  val up = Bool()
  val left = Bool()
  val right = Bool()
}

/**
 * The main Gameboy module.
 *
 * External interfaces: cartridge, PPU output
 *
 * The "external bus" contains the cartridge (rom and ram) and work ram.
 * Video ram is on a separate bus (OAM DMA with it doesn't block wram).
 * The peripherals (0xFF**) are separate, and the OAM is also special.
 */
class Gameboy(config: Gameboy.Configuration) extends Module {
  val io = IO(new Bundle {
    // Control signals
    val clockConfig = new ClockConfig

    // Regular I/O signals
    val cartridge = new CartridgeIo()
    val ppu = new PpuOutput()
    val joypad = Input(new JoypadState)
    val apu = new ApuOutput
    val serial = new SerialIo

    // Internal state signals
    val tCycle = Output(UInt(2.W))
    val cpuDebug = Output(new Cpu.DebugState)
  })

  // Clock control module
  val clockControl = Module(new ClockControl)
  io.clockConfig <> clockControl.io.clockConfig
  io.tCycle := clockControl.io.clocker.tCycle

  // Module: System control
  val systemControl = Module(new SystemControl(config))

  // Module: CPU
  val cpu = Module(new Cpu(config))
  cpu.io.clocker := clockControl.io.clocker
  cpu.io.interruptRequest := 0.U.asTypeOf(new Cpu.InterruptFlags)
  io.cpuDebug := cpu.io.debugState

  // Module: PPU
  val ppu = Module(new Ppu(config))
  ppu.io.clocker := clockControl.io.clocker
  io.ppu := ppu.io.output
  cpu.io.interruptRequest.vblank := ppu.io.vblankIrq
  cpu.io.interruptRequest.lcdStat := ppu.io.statIrq

  // Module: OAM DMA
  val oamDma = Module(new OamDma)
  val oamDmaData = WireDefault(0xFF.U(8.W))
  oamDma.io.clocker := clockControl.io.clocker
  val oamDmaSelectExternal = oamDma.io.dmaAddress(15, 8) < 0x80.U || oamDma.io.dmaAddress(15, 8) >= 0xA0.U
  val oamDmaSelectVram = !oamDmaSelectExternal

  // Module: APU
  val apu = Module(new Apu(config))
  apu.io.clocker := clockControl.io.clocker
  io.apu := apu.io.output

  // Memories
  val workRam = Module(new SinglePortRam(8 * 1024)) // DMG: 0xC000 to 0xDFFF
  val videoRam = Module(new SinglePortRam(8 * 1024)) // 0x8000 to 0x9FFF
  val oam = Module(new SinglePortRam(160)) // 0xFE00 to 0xFE9F
  val highRam = Module(new HighRam)

  // Peripheral: Timer
  val timer = Module(new Timer)
  timer.io.clocker := clockControl.io.clocker
  cpu.io.interruptRequest.timer := timer.io.interruptRequest
  apu.io.divApu := timer.io.divApu

  // Peripheral: Joypad
  val joypad = Module(new Joypad)
  joypad.io.clocker := clockControl.io.clocker
  joypad.io.state := io.joypad
  cpu.io.interruptRequest.joypad := joypad.io.interruptRequest

  // Peripheral: Serial
  val serial = Module(new Serial(config))
  serial.io.clocker := clockControl.io.clocker
  serial.io.divSerial := timer.io.divSerial
  cpu.io.interruptRequest.serial := serial.io.interruptRequest
  io.serial <> serial.io.serial

  // Boot rom
  val bootRom = Module(new BootRom(config))
  bootRom.io.address := cpu.io.memAddress
  bootRom.io.mapped := systemControl.io.bootRomMapped

  // External bus read/write logic
  val busAddress = WireDefault(cpu.io.memAddress)
  val busDataWrite = WireDefault(cpu.io.memDataOut)
  val busMemEnable = WireDefault(cpu.io.memEnable)
  val busMemWrite = WireDefault(cpu.io.memWrite)
  val cartRomSelect = busAddress >= 0x0000.U && busAddress < 0x8000.U
  val cartRamSelect = busAddress >= 0xA000.U && busAddress < 0xC000.U
  val workRamSelect = busAddress >= 0xC000.U && busAddress < 0xFE00.U
  val busDataRead = Mux1H(Seq(
    (cartRomSelect || cartRamSelect, io.cartridge.dataRead),
    (workRamSelect, workRam.io.dataRead),
  ))
  when (oamDma.io.active && oamDmaSelectExternal) {
    busAddress := oamDma.io.dmaAddress
    busMemEnable := true.B
    busMemWrite := false.B
    busDataWrite := DontCare
    oamDmaData := busDataRead
  }
  val cpuExternalBusSelect =
    cpu.io.memAddress < 0x8000.U ||
    (cpu.io.memAddress >= 0xA000.U && cpu.io.memAddress < 0xFE00.U)

  // Cartridge access signals
  io.cartridge.enable := busMemEnable && (cartRomSelect || cartRamSelect)
  io.cartridge.deadline := clockControl.io.clocker.tCycle === 3.U
  io.cartridge.write := busMemWrite
  io.cartridge.chipSelect := cartRomSelect
  io.cartridge.dataWrite := busDataWrite
  io.cartridge.address := busAddress
  when (cartRomSelect || cartRamSelect) { busDataRead := io.cartridge.dataRead }

  // Work ram
  workRam.io.address := busAddress(12, 0)
  workRam.io.enabled := busMemEnable && workRamSelect
  workRam.io.write := busMemWrite && clockControl.io.clocker.phiPulse
  workRam.io.dataWrite := busDataWrite

  // Video ram
  // PPU locks it during pixel fetch mode (not hblank, vblank, oam search)
  // OAM DMA locks it if reading from this region
  // Priority: OAM DMA > PPU > CPU
  val cpuVideoRamSelect = cpu.io.memAddress >= 0x8000.U && cpu.io.memAddress < 0xA000.U
  when (oamDma.io.active && oamDmaSelectVram) {
    videoRam.io.address := oamDma.io.dmaAddress(12, 0)
    videoRam.io.enabled := true.B
    videoRam.io.write := false.B
    videoRam.io.dataWrite := DontCare
    oamDmaData := videoRam.io.dataRead
  } .elsewhen (ppu.io.vramEnabled) {
    // PPU has control of VRAM bus
    videoRam.io.address := ppu.io.vramAddress
    videoRam.io.enabled := true.B
    videoRam.io.write := false.B
    videoRam.io.dataWrite := DontCare
  } .otherwise {
    // CPU is controlling VRAM bus
    videoRam.io.address := cpu.io.memAddress
    videoRam.io.enabled := cpu.io.memEnable && cpuVideoRamSelect
    videoRam.io.write := cpu.io.memWrite && clockControl.io.clocker.phiPulse
    videoRam.io.dataWrite := cpu.io.memDataOut
  }
  ppu.io.vramDataRead := videoRam.io.dataRead

  // Oam ram
  // PPU locks it during oam dma and pixel fetch mode (not hblank, vblank)
  // OAM DMA locks it if writing
  // Priority: OAM DMA > PPU > CPU
  val cpuOamSelect = cpu.io.memAddress >= 0xFE00.U && cpu.io.memAddress < 0xFEA0.U
  when (oamDma.io.active) {
    // OAM DMA is writing
    oam.io.address := oamDma.io.dmaAddress(7, 0)
    oam.io.enabled := true.B
    oam.io.write := clockControl.io.clocker.phiPulse
    oam.io.dataWrite := oamDmaData
  } .elsewhen (ppu.io.oamEnabled) {
    // PPU has control of OAM bus
    oam.io.address := ppu.io.oamAddress
    oam.io.enabled := true.B
    oam.io.write := false.B
    oam.io.dataWrite := DontCare
  } .otherwise {
    // CPU is controlling OAM bus
    oam.io.address := cpu.io.memAddress(7, 0)
    oam.io.enabled := cpu.io.memEnable && cpuOamSelect
    oam.io.write := cpu.io.memWrite && clockControl.io.clocker.phiPulse
    oam.io.dataWrite := cpu.io.memDataOut
  }
  ppu.io.oamDataRead := oam.io.dataRead

  // Peripheral bus
  val peripherals = Seq(
    serial.io,
    highRam.io,
    timer.io,
    ppu.io.registers,
    oamDma.io,
    joypad.io,
    systemControl.io,
    apu.io.reg,
  )
  val peripheralSelect = cpu.io.memAddress(15, 8) === 0xFF.U
  for (peripheral <- peripherals) {
    peripheral.address := cpu.io.memAddress(7, 0)
    peripheral.enabled := peripheralSelect && cpu.io.memEnable
    peripheral.write := cpu.io.memWrite && clockControl.io.clocker.phiPulse
    peripheral.dataWrite := cpu.io.memDataOut
  }
  val peripheralValid = peripheralSelect && VecInit(peripherals.map(_.valid)).asUInt.orR
  val peripheralDataRead = Mux1H(peripherals.map(p => (p.valid, p.dataRead)))

  // CPU connection to the busses
  val cpuInputs = Seq(
    (bootRom.io.valid, bootRom.io.dataRead),
    (cpuExternalBusSelect && !bootRom.io.valid, busDataRead),
    (peripheralValid, peripheralDataRead),
    (cpuVideoRamSelect, videoRam.io.dataRead),
    (cpuOamSelect, oam.io.dataRead),
  )
  val cpuInputsValid = VecInit(cpuInputs.map(_._1)).asUInt.orR
  cpu.io.memDataIn := Mux1H(cpuInputs ++ Seq((!cpuInputsValid, 0xFF.U)))
}

object Gameboy extends App {
  object Model extends Enumeration {
    type Model = Value
    val Dmg = Value
    val Cgb = Value

    implicit class ModelValue(model: Value) {
      def isCgb: Boolean = model match {
        case Cgb => true
        case _ => false
      }
    }
  }
  import Gameboy.Model.Model

  case class Configuration(
    /** Whether to skip the boot rom */
    skipBootrom: Boolean,
    /** Whether to optimize for simulation, at the cost of potentially breaking synthesis. */
    optimizeForSimulation: Boolean = false,
    /** The model of Gameboy */
    model: Model = Model.Dmg,
  )
}