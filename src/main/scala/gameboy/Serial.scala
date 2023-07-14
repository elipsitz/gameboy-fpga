package gameboy

import chisel3._
import chisel3.util._
import gameboy.Timer.RegisterControl

class SerialDebug extends Bundle {
  val regData = UInt(8.W)
  val regBitsLeft = UInt(3.W)
  val regEnable = Bool()
  val regClockMode = Bool()
  val clockIn = Bool()
  val clockOut = Bool()
  val dataOut = Bool()
  val dataIn = Bool()
}

class SerialIo extends Bundle {
  /** Output bit */
  val out = Output(Bool())
  /** Input bit */
  val in = Input(Bool())

  /** True if clockOut is being used, False if clockIn */
  val clockEnable = Output(Bool())
  /** Output clock signal (if clockEnable) */
  val clockOut = Output(Bool())
  /** Input clock signal (if !clockEnable) */
  val clockIn = Input(Bool())
}

/**
 * Serial data transfer
 *
 * We shift out a new bit on falling clock edge, shift in a bit on the rising edge
 */
class Serial(config: Gameboy.Configuration) extends Module {
  val io = IO(new PeripheralAccess {
    val clocker = Input(new Clocker)

    val interruptRequest = Output(Bool())
    /**
     * 16384 Hz timer (x2 in double speed)
     * This is the frequency of edges, not changes. Each edge we invert the internal clock signal,
     * because we want a 8192 Hz clock.
     */
    val divSerial = Input(Bool())

    val serial = new SerialIo()

    /** Debug output */
    val debug = Output(new SerialDebug)
  })

  val regData = RegInit(0.U(8.W))
  val regEnable = RegInit(false.B)
  /** false: external clock, true: internal clock */
  val regClockMode = RegInit(false.B)
  val regBitsLeft = RegInit(0.U(3.W))

  // Register memory mapping
  when (io.enabled && io.write) {
    switch (io.address) {
      // 0xFF01 - SB: Serial transfer data
      is (0x01.U) { regData := io.dataWrite }
      // 0xFF02 - SC: Serial transfer control
      is (0x02.U) {
        val enabled = io.dataWrite(7)
        val clockMode = io.dataWrite(0)
        regEnable := enabled
        regClockMode := clockMode

        // Start a transfer
        when (enabled) {
          regBitsLeft := 0.U // "8"

          if (config.optimizeForSimulation) {
            // Debug serial
            printf(cf"${regData}%c")
          }
        }
      }
    }
  }
  io.valid := (io.address === 0x01.U) || (io.address === 0x02.U)
  io.dataRead := 0xFF.U
  when (io.enabled && !io.write) {
    switch (io.address) {
      is (0x01.U) { io.dataRead := regData }
      is (0x02.U) { io.dataRead := Cat(regEnable, "b111111".U(6.W), regClockMode) }
    }
  }

  // Clock
  val clockOut = RegInit(true.B)
  io.serial.clockOut := clockOut
  io.serial.clockEnable := regClockMode
  val clockSignal = Mux(regClockMode, clockOut, io.serial.clockIn)
  val prevClockSignal = RegEnable(clockSignal, io.clocker.enable)
  val clockRising = !prevClockSignal && clockSignal
  val clockFalling = prevClockSignal && !clockSignal

  // Internal clock
  when (io.clocker.enable && regEnable && regClockMode && RegEnable(io.divSerial, io.clocker.enable) && !io.divSerial) {
    clockOut := !clockOut
  }

  // Serial operation
  io.interruptRequest := false.B
  val regOut = RegInit(1.U(1.W))
  io.serial.out := regOut
  when (io.clocker.enable && clockFalling) {
    regOut := regData(7)
    regData := regData << 1
  }
  when (io.clocker.enable && clockRising) {
    regData := Cat(regData(7, 1), io.serial.in)

    regBitsLeft := regBitsLeft - 1.U
    when (regBitsLeft === 1.U) {
      // Transfer complete, we're going down to 0
      regEnable := false.B
      io.interruptRequest := true.B
    }
  }

  // Debug output
  io.debug.regData := regData
  io.debug.regBitsLeft := regBitsLeft
  io.debug.regEnable := regEnable
  io.debug.regClockMode := regClockMode
  io.debug.clockIn := io.serial.clockIn
  io.debug.clockOut := clockOut
  io.debug.dataOut := regData
  io.debug.dataIn := io.serial.in
}
