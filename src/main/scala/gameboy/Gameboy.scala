package gameboy

import chisel3._
import chisel3.util._
import gameboy.cpu.{Cpu, TestableCpu}

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
 * External interfaces: cartridge
 *
 * The "external bus" contains the cartridge (rom and ram), work ram, and video ram.
 * The peripherals (0xFF**) are separate, and the OAM is also special.
 */
class Gameboy extends Module {
  val io = IO(new Bundle {
    val cartridge = new CartridgeIo()
  })

  val cpu = Module(new Cpu())
  val workRam = SyncReadMem(8 * 1024, UInt(8.W)) // DMG: 0xC000 to 0xDFFF
  val debugSerial = Module(new DebugSerial)
  val highRam = Module(new HighRam)

  val peripherals = Seq(debugSerial.io, highRam.io)

  // External bus read/write logic
  val busAddress = cpu.io.memAddress
  val busDataWrite = cpu.io.memDataOut
  val busDataRead = WireDefault(0.U(8.W))
  val busMemEnable = cpu.io.memEnable
  val busMemWrite = cpu.io.memWrite
  val phiPulse = cpu.io.tCycle === 3.U

  // Cartridge access signals
  val cartRomSelect = busAddress >= 0x0000.U && busAddress < 0x8000.U
  val cartRamSelect = busAddress >= 0xA000.U && busAddress < 0xC000.U
  io.cartridge.writeEnable := (cartRomSelect || cartRamSelect) && busMemEnable && busMemWrite
  io.cartridge.readEnable := !io.cartridge.writeEnable
  io.cartridge.chipSelect := !cartRomSelect
  io.cartridge.dataWrite := busDataWrite
  io.cartridge.address := busAddress
  when (cartRomSelect || cartRamSelect) { busDataRead := io.cartridge.dataRead }

  // External bus
  when (busMemEnable && busAddress >= 0xC000.U && busAddress < 0xFE00.U) {
    val workRamAddress = busAddress(12, 0)
    when (busMemWrite && phiPulse) { workRam.write(workRamAddress, busDataWrite) }
      .otherwise { busDataRead := workRam.read(workRamAddress) }
  }

  // Peripheral bus
  val peripheralDataRead = WireDefault(0xFF.U(8.W))
  val peripheralActive = cpu.io.memAddress(15, 8) === 0xFF.U
  for (peripheral <- peripherals) {
    peripheral.address := cpu.io.memAddress(7, 0)
    peripheral.enabled := peripheralActive && cpu.io.memEnable
    peripheral.write := cpu.io.memWrite && phiPulse
    peripheral.dataWrite := cpu.io.memDataOut
    when (peripheral.valid) { peripheralDataRead := peripheral.dataRead }
  }

  // CPU connection to the busses
  cpu.io.memDataIn := 0xFF.U
  when (cpu.io.memAddress < 0xFE00.U) {
    cpu.io.memDataIn := busDataRead
  } .elsewhen (peripheralActive) {
    cpu.io.memDataIn := peripheralDataRead
  }
}

object Gameboy extends App {
  emitVerilog(new Gameboy(), args)
  emitVerilog(new TestableCpu(), args)
}