package gameboy

import chisel3._
import chisel3.util._

/** Miscellaneous 'system control' registers. */
class SystemControl(config: Gameboy.Configuration) extends Module {
  val io = IO(new PeripheralAccess {
    /** True if the boot rom is mapped. */
    val bootRomMapped = Output(Bool())
    /** True if this is a CGB and CGB mode has been enabled. */
    val cgbMode = Output(Bool())
  })

  val regBootOff = RegInit(config.skipBootrom.B)
  val regCgbMode = RegInit(config.model.isCgb.B)
  io.bootRomMapped := !regBootOff
  io.cgbMode := regCgbMode

  // System control registers
  when (io.enabled && io.write) {
    switch(io.address) {
      // 0xFF50 - Boot Rom control
      is (0x50.U) {
        when (!regBootOff) { regBootOff := io.dataWrite(0) }
      }

      // 0xFF4C - KEY0 (CGB only)
      is (0x4C.U) {
        if (config.model.isCgb) {
          when (!regBootOff) {
            regCgbMode := !io.dataWrite(2)
            printf(cf"CGB mode: ${!io.dataWrite(2)}\n")
          }
        }
      }
    }
  }
  if (config.model.isCgb) {
    io.valid := (io.address === 0x50.U) || (io.address === 0x4C.U)
  } else {
    io.valid := (io.address === 0x50.U)
  }
  io.dataRead := 0xFF.U
  when (io.enabled && !io.write) {
    switch (io.address) {
      is (0x50.U) {
        io.dataRead := Cat("b1111111".U(7.W), regBootOff)
      }
    }
  }
}
