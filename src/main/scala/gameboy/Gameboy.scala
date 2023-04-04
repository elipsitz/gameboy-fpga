package gameboy

import chisel3._
import chisel3.util._

import gameboy.cpu.Cpu

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
  val dataIn = Input(UInt(8.W))
  /** Data out (writing) */
  val dataOut = Output(UInt(8.W))
}

class Gameboy extends Module {
  val io = IO(new Bundle {
    val cartridge = new CartridgeIo()
  })

  val cpu = Module(new Cpu())
  val workRam = SyncReadMem(8 * 1024, UInt(8.W)) // DMG: 0xC000 to 0xDFFF
  val highRam = SyncReadMem(128, UInt(8.W)) // Only 127 bytes are accessible

  // Bus read/write logic
  val busAddress = cpu.io.memAddress
  val busDataWrite = cpu.io.memDataOut
  val busDataRead = WireDefault(0.U(8.W))
  cpu.io.memDataIn := busDataRead
  val busMemEnable = cpu.io.memEnable
  val busMemWrite = cpu.io.memWrite

  // Peripheral enables
  val cartRomEnable = busMemEnable && (busAddress >= 0x0000.U && busAddress < 0x8000.U)
  val cartRamEnable = busMemEnable && (busAddress >= 0xA000.U && busAddress < 0xC000.U)
  val workRamEnable = busMemEnable && (busAddress >= 0xC000.U && busAddress < 0xFE00.U)
  val highRamEnable = busMemEnable && (busAddress >= 0xFF80.U && busAddress < 0xFFFF.U)

  // External bus (cartridge) signals
  io.cartridge.writeEnable := (cartRomEnable || cartRamEnable) && busMemWrite
  io.cartridge.readEnable := !io.cartridge.writeEnable
  io.cartridge.chipSelect := !cartRamEnable
  io.cartridge.dataOut := busDataWrite
  io.cartridge.address := busAddress
  when (cartRomEnable || cartRomEnable) { busDataRead := io.cartridge.dataIn }

  // Read and write from the peripherals
  when (workRamEnable) {
    val workRamAddress = busAddress(12, 0)
    when (busMemWrite) { workRam.write(workRamAddress, busDataWrite) }
      .otherwise { busDataRead := workRam.read(workRamAddress) }
  }
  when (highRamEnable) {
    val highRamAddress = busAddress(6, 0)
    when (busMemWrite) { highRam.write(highRamAddress, busDataWrite) }
      .otherwise { busDataRead := highRam.read(highRamAddress) }
  }
}

object Gameboy extends App {
  emitVerilog(new Gameboy(), args)
}