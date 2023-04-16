package gameboy

import chisel3._
import chisel3.util._

class Joypad extends Module {
  val io = IO(new PeripheralAccess {
    val state = Input(new JoypadState())
  })

  val selectDirection = RegInit(false.B)
  val selectAction = RegInit(false.B)

  io.dataRead := Cat(
    "b1100".U(4.W),
    !((io.state.down && selectDirection) || (io.state.start && selectAction)),
    !((io.state.up && selectDirection) || (io.state.select && selectAction)),
    !((io.state.left && selectDirection) || (io.state.b && selectAction)),
    !((io.state.right && selectDirection) || (io.state.a && selectAction)),
  )

  io.valid := false.B
  when (io.enabled && io.address === 0x00.U) {
    io.valid := true.B
    when (io.write) {
      selectDirection := !io.dataWrite(4)
      selectAction := !io.dataWrite(5)
    }
  }
}