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

    val vramBank = Output(UInt(1.W))
    val wramBank = Output(UInt(3.W))
  })

  val regBootOff = RegInit(config.skipBootrom.B)
  val regCgbMode = RegInit(config.model.isCgb.B)
  val regVramBank = RegInit(0.U(1.W))
  val regWramBank = RegInit(0.U(3.W))
  io.bootRomMapped := !regBootOff
  io.cgbMode := regCgbMode
  io.vramBank := regVramBank
  io.wramBank := Mux(regWramBank === 0.U, 1.U, regWramBank)

  // System control registers
  when (io.enabled && io.write) {
    switch (io.address) {
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

      // FF70 - SVBK (CGB Mode only): WRAM bank
      is (0x70.U) {
        when (regCgbMode) { regWramBank := io.dataWrite(2, 0) }
      }

      // FF4F - VBK (CGB Mode only): VRAM bank
      is (0x4F.U) {
        when (regCgbMode) { regVramBank := io.dataWrite(0) }
      }
    }
  }
  when (regCgbMode) {
    io.valid := Seq(0x50, 0x4C, 0x70, 0x4F).map(io.address === _.U).reduce((a, b) => a || b)
  } .otherwise {
    io.valid := (io.address === 0x50.U)
  }
  io.dataRead := 0xFF.U
  when (io.enabled && !io.write) {
    switch (io.address) {
      is (0x50.U) { io.dataRead := Cat("b1111111".U(7.W), regBootOff) }
      is (0x70.U) { io.dataRead := Cat("b11111".U(5.W), regWramBank) }
      is (0x4F.U) { io.dataRead := Cat("b1111111".U(7.W), regVramBank) }
    }
  }
}
