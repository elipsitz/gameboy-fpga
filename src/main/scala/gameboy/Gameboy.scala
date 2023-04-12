package gameboy

import chisel3._
import chisel3.util._
import gameboy.cpu.{Cpu, TestableCpu}
import gameboy.ppu.{Ppu, PpuOutput}

class CartridgeIo extends Bundle {
  /** Cartridge address selection */
  val address = Output(UInt(16.W))
  /** Read flag (active high -- real cartridge is low) */
  val readEnable = Output(Bool())
  /** Write flag (active high -- real cartridge is low) */
  val writeEnable = Output(Bool())
  /** Chip select (high for ROM, low for RAM) */
  val chipSelect = Output(Bool())
  /** Data in (reading) */
  val dataRead = Input(UInt(8.W))
  /** Data out (writing) */
  val dataWrite = Output(UInt(8.W))
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
class Gameboy extends Module {
  val io = IO(new Bundle {
    val cartridge = new CartridgeIo()
    val ppu = new PpuOutput()
  })

  // Module: CPU
  val cpu = Module(new Cpu())
  cpu.io.interruptRequest := 0.U.asTypeOf(new Cpu.InterruptFlags)
  val phiPulse = cpu.io.tCycle === 3.U

  // Module: PPU
  val ppu = Module(new Ppu())
  io.ppu := ppu.io.output

  // Memories
  val workRam = Module(new SinglePortRam(8 * 1024)) // DMG: 0xC000 to 0xDFFF
  val videoRam = Module(new SinglePortRam(8 * 1024)) // 0x8000 to 0x9FFF
  val oam = Module(new SinglePortRam(160)) // 0xFE00 to 0xFE9F

  // Basic peripherals
  val debugSerial = Module(new DebugSerial)
  val highRam = Module(new HighRam)

  // Peripheral: Timer
  val timer = Module(new Timer)
  timer.io.phiPulse := phiPulse
  cpu.io.interruptRequest.timer := timer.io.interruptRequest

  // External bus read/write logic
  val busAddress = cpu.io.memAddress
  val busDataWrite = cpu.io.memDataOut
  val busMemEnable = cpu.io.memEnable
  val busMemWrite = cpu.io.memWrite
  val cartRomSelect = busAddress >= 0x0000.U && busAddress < 0x8000.U
  val cartRamSelect = busAddress >= 0xA000.U && busAddress < 0xC000.U
  val workRamSelect = busAddress >= 0xC000.U && busAddress < 0xFE00.U
  val busDataRead = Mux1H(Seq(
    (cartRomSelect || cartRamSelect, io.cartridge.dataRead),
    (workRamSelect, workRam.io.dataRead),
  ))
  val busReadValid = cartRomSelect || cartRamSelect || workRamSelect

  // Cartridge access signals
  io.cartridge.writeEnable := (cartRomSelect || cartRamSelect) && busMemEnable && busMemWrite
  io.cartridge.readEnable := !io.cartridge.writeEnable
  io.cartridge.chipSelect := !cartRomSelect
  io.cartridge.dataWrite := busDataWrite
  io.cartridge.address := busAddress
  when (cartRomSelect || cartRamSelect) { busDataRead := io.cartridge.dataRead }

  // Work ram
  workRam.io.address := busAddress(12, 0)
  workRam.io.enabled := busMemEnable && workRamSelect
  workRam.io.write := busMemWrite && phiPulse
  workRam.io.dataWrite := busDataWrite

  // Video ram
  // TODO allow PPU and OAM DMA to access this
  // PPU locks it during pixel fetch mode (not hblank, vblank, oam search)
  // OAM DMA locks it if reading from this region
  // Priority: OAM DMA > PPU > CPU
  val videoRamSelect = busAddress >= 0x8000.U && busAddress < 0xA000.U
  videoRam.io.address := cpu.io.memAddress
  videoRam.io.enabled := cpu.io.memEnable && videoRamSelect
  videoRam.io.write := cpu.io.memWrite && phiPulse
  videoRam.io.dataWrite := cpu.io.memDataOut

  // Oam ram
  // TODO allow PPU and OAM DMA to access this
  // PPU locks it during oam dma and pixel fetch mode (not hblank, vblank)
  // OAM DMA locks it if writing
  // Priority: OAM DMA > PPU > CPU
  val oamSelect = busAddress >= 0xFE00.U && busAddress < 0xFEA0.U
  oam.io.address := cpu.io.memAddress(7, 0)
  oam.io.enabled := cpu.io.memEnable && oamSelect
  oam.io.write := cpu.io.memWrite && phiPulse
  oam.io.dataWrite := cpu.io.memDataOut

  // Peripheral bus
  val peripherals = Seq(debugSerial.io, highRam.io, timer.io)
  val peripheralSelect = cpu.io.memAddress(15, 8) === 0xFF.U
  for (peripheral <- peripherals) {
    peripheral.address := cpu.io.memAddress(7, 0)
    peripheral.enabled := peripheralSelect && cpu.io.memEnable
    peripheral.write := cpu.io.memWrite && phiPulse
    peripheral.dataWrite := cpu.io.memDataOut
  }
  val peripheralValid = VecInit(peripherals.map(_.valid)).asUInt.orR
  val peripheralDataRead = Mux1H(peripherals.map(p => (p.valid, p.dataRead)))

  // CPU connection to the busses
  // TODO bootrom
  val cpuInputs = Seq(
    (busReadValid, busDataRead),
    (peripheralValid, peripheralDataRead),
    (videoRamSelect, videoRam.io.dataRead),
    (oamSelect, oam.io.dataRead),
  )
  val cpuInputsValid = VecInit(cpuInputs.map(_._1)).asUInt.orR
  cpu.io.memDataIn := Mux1H(cpuInputs ++ Seq((!cpuInputsValid, 0xFF.U)))
}

object Gameboy extends App {
  emitVerilog(new Gameboy(), args)
//  emitVerilog(new TestableCpu(), args)
}