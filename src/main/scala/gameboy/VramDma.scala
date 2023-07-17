package gameboy

import chisel3._
import chisel3.util._
import VramDma._

object VramDma {
  private val ModeGeneral = 0.U
  private val ModeHblank = 1.U
}

/**
 * CGB-only general purpose and hblank DMA
 *
 * TODO: is this supposed to behave differently if the LCD is off?
 * */
class VramDma extends Module {
  val io = IO(new PeripheralAccess {
    val clocker = Input(new Clocker)
    val cgbMode = Input(Bool())

    /** Whether the PPU is in hblank */
    val hblank = Input(Bool())

    /** Whether VRAM DMA is active (i.e. actively transferring data */
    val active = Output(Bool())

    val addressSource = Output(UInt(16.W))
    val addressDest = Output(UInt(13.W))
  })

  /// Whether DMA is actively running.
  val regActive = RegInit(false.B)
  io.active := regActive
  /// Whether DMA is enabled (even if not actively running yet).
  val regEnabled = RegInit(false.B)
  /// Current source address (excluding the bottom 4 bits)
  val regSource = RegInit(0.U(12.W))
  /// Current destination address (excluding the bottom 4 bits)
  val regDest = RegInit(0.U(9.W))
  /// Number of 16-byte chunks left to transfer (minus 1, so 0x00 is 1 left).
  val regChunksLeft = RegInit(0.U(7.W))
  /// Number of bytes within the 16-byte chunk
  val regBytes = RegInit(0.U(4.W))
  /// DMA mode
  val regMode = RegInit(0.U(1.W))

  io.addressSource := Cat(regSource, regBytes)
  io.addressDest := Cat(regDest, regBytes)

  // In HDMA, each transfer starts on the rising edge of hblank.
  // We *also* align to a phi pulse to make things simpler.
  val prevHblank = RegEnable(io.hblank, io.clocker.phiPulse)
  when (io.clocker.phiPulse && regMode === ModeHblank && regEnabled && (!prevHblank && io.hblank) && !regActive) {
    regActive := true.B
  }

  when (io.clocker.pulseVramDma && regActive) {
    // Do a transfer.
    regBytes := regBytes + 1.U
    when (regBytes === 15.U) {
      // Done with this chunk of bytes.
      regChunksLeft := regChunksLeft - 1.U
      // regSource/regDest are in multiples of chunks (16 bytes)
      regSource := regSource + 1.U
      regDest := regDest + 1.U
      when (regMode === ModeHblank) {
        regActive := false.B
      }
      when (regChunksLeft === 0.U) {
        // Done with the whole transfer.
        regEnabled := false.B
        regActive := false.B
      }
    }
  }

  // Register memory mapping
  when (io.enabled && io.write && io.cgbMode) {
    switch (io.address) {
      // 0xFF51 - VRAM DMA source high
      is (0x51.U) { regSource := Cat(io.dataWrite, regSource(3, 0)) }
      // 0xFF52 - VRAM DMA source low
      is (0x52.U) { regSource := Cat(regSource(11, 4), io.dataWrite(7, 4)) }
      // 0xFF53 - VRAM DMA destination high
      is (0x53.U) { regDest := Cat(io.dataWrite(4, 0), regDest(3, 0)) }
      // 0xFF54 - VRAM DMA destination low
      is (0x54.U) { regDest := Cat(regDest(8, 4), io.dataWrite(7, 4)) }
      // 0xFF55 - VRAM DMA length/mode/start
      is (0x55.U) {
        when (regEnabled) {
          when (!io.dataWrite(7)) {
            // Terminate a transfer.
            regEnabled := false.B
          }
        } .otherwise {
          // Start a transfer.
          val mode = io.dataWrite(7)
          regMode := mode
          regChunksLeft := io.dataWrite(6, 0)
          regEnabled := true.B
          regBytes := 0.U
          when (mode === ModeGeneral || io.hblank) {
            // Start the transfer for general dma, or if hblank and we're already in hblank.
            regActive := true.B
          }
        }
      }
    }
  }
  io.valid := io.cgbMode && (io.address === 0x55.U)
  io.dataRead := DontCare
  when (io.enabled && !io.write) {
    switch (io.address) {
      is (0x55.U) {
        // Bit 7: 1 if NOT active, 0 if active.
        // Bits 0-6: number of chunks remaining (minus 1)
        io.dataRead := Cat(!regEnabled, regChunksLeft)
      }
    }
  }
}
