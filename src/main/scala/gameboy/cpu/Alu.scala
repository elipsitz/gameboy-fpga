package gameboy.cpu

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._

object Alu {
  class Flags extends Bundle {
    val c = Bool()
    val h = Bool()
    val n = Bool()
    val z = Bool()
  }

  object Opcode extends ChiselEnum {
    val add = Value("b00000".U)
    val adc = Value("b00001".U)
    val sub = Value("b00010".U)
    val sbc = Value("b00011".U)
    val and = Value("b00100".U)
    val xor = Value("b00101".U)
    val or = Value("b00110".U)
    val cp = Value("b00111".U)

    val rlca = Value("b01000".U)
    val rrca = Value("b01001".U)
    val rla = Value("b01010".U)
    val rra = Value("b01011".U)
    val daa = Value("b01100".U)
    val cpl = Value("b01101".U)
    val scf = Value("b01110".U)
    val ccf = Value("b01111".U)

    val rlc = Value("b10000".U)
    val rrc = Value("b10001".U)
    val rl = Value("b10010".U)
    val rr = Value("b10011".U)
    val sla = Value("b10100".U)
    val sra = Value("b10101".U)
    val swap = Value("b10110".U)
    val srl = Value("b10111".U)

    val copyA = Value("b11000".U)
    val copyB = Value("b11001".U)
    val incB = Value("b11010".U)
    val decB = Value("b11011".U)
    val unused = Value("b11100".U)
    val bit = Value("b11101".U)
    val res = Value("b11110".U)
    val set = Value("b11111".U)
  }
}

class Alu extends Module {
  val io = IO(new Bundle {
    /** The first operand */
    val a = Input(UInt(8.W))
    /** The second operand */
    val b = Input(UInt(8.W))
    /** Operation type */
    val op = Input(Alu.Opcode())
    /** Input flags  */
    val flagIn = Input(new Alu.Flags())
    /** Input bit index (for CB single-bit ops) */
    val bitIndex = Input(UInt(3.W))

    /** Alu output */
    val out = Output(UInt(8.W))
    /** Output flags */
    val flagOut = Output(new Alu.Flags())
  })

  io.out := 0.U
  io.flagOut := io.flagIn

  switch (io.op) {
    is (Alu.Opcode.copyA) { io.out := io.a }
    is (Alu.Opcode.copyB) { io.out := io.b }
    is (Alu.Opcode.incB) {
      io.out := io.b + 1.U
      io.flagOut.h := io.b(3, 0) === "b1111".U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.decB) {
      io.out := io.b - 1.U
      io.flagOut.h := io.b(3, 0) === "b0000".U
      io.flagOut.n := 1.U
      io.flagOut.z := io.out === 0.U
    }
    is (Alu.Opcode.add, Alu.Opcode.adc) {
      val carry = Mux(io.op === Alu.Opcode.adc, io.flagIn.c, 0.U)
      val lo = Cat(0.U(1.W), io.a(3, 0)) + Cat(0.U(1.W), io.b(3, 0)) + Cat(0.U(4.W), carry)
      val hi = Cat(0.U(1.W), io.a(7, 4)) + Cat(0.U(1.W), io.b(7, 4)) + Cat(0.U(4.W), lo(4))
      io.out := Cat(hi(3, 0), lo(3, 0))
      io.flagOut.c := hi(4)
      io.flagOut.h := lo(4)
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.sub, Alu.Opcode.sbc, Alu.Opcode.cp) {
      val carry = Mux(io.op === Alu.Opcode.sbc, io.flagIn.c, 0.U)
      val lo = Cat(0.U(1.W), io.a(3, 0)) + (~Cat(0.U(1.W), io.b(3, 0))).asUInt + Cat(0.U(4.W), ~carry)
      val hi = Cat(0.U(1.W), io.a(7, 4)) + (~Cat(0.U(1.W), io.b(7, 4))).asUInt + Cat(0.U(4.W), ~lo(4))
      io.out := Mux(io.op === Alu.Opcode.cp, io.a, Cat(hi(3, 0), lo(3, 0)))
      io.flagOut.c := hi(4)
      io.flagOut.h := lo(4)
      io.flagOut.n := 1.U
      io.flagOut.z := Cat(hi(3, 0), lo(3, 0)) === 0.U
    }
    is(Alu.Opcode.and) {
      io.out := io.a & io.b
      io.flagOut.c := 0.U
      io.flagOut.h := 1.U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.xor) {
      io.out := io.a ^ io.b
      io.flagOut.c := 0.U
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.or) {
      io.out := io.a | io.b
      io.flagOut.c := 0.U
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.rlca) {
      io.out := Cat(io.a(6, 0), io.a(7))
      io.flagOut.c := io.a(7)
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := 0.U
    }
    is(Alu.Opcode.rrca) {
      io.out := Cat(io.a(0), io.a(7, 1))
      io.flagOut.c := io.a(0)
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := 0.U
    }
    is(Alu.Opcode.rla) {
      io.out := Cat(io.a(6, 0), io.flagIn.c)
      io.flagOut.c := io.a(7)
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := 0.U
    }
    is(Alu.Opcode.rra) {
      io.out := Cat(io.flagIn.c, io.a(7, 1))
      io.flagOut.c := io.a(0)
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := 0.U
    }
    is(Alu.Opcode.daa) {
      val lo = Mux(io.flagIn.h | (!io.flagIn.n & (io.a(3, 0) > 9.U)), 6.U(4.W), 0.U(4.W))
      val hi = Mux(io.flagIn.c | (!io.flagIn.n & (io.a > 0x99.U)), 6.U(4.W), 0.U(4.W))
      io.out := Mux(
        io.flagIn.n,
        io.a - Cat(hi, lo),
        io.a + Cat(hi, lo),
      )
      io.flagOut.c := hi =/= 0.U
      io.flagOut.h := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.cpl) {
      io.out := ~io.a
      io.flagOut.h := 1.U
      io.flagOut.n := 1.U
    }
    is(Alu.Opcode.scf) {
      io.out := io.a
      io.flagOut.c := 1.U
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
    }
    is(Alu.Opcode.ccf) {
      io.out := io.a
      io.flagOut.c := !io.flagIn.c
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
    }
    is(Alu.Opcode.rlc) {
      io.out := Cat(io.a(6, 0), io.a(7))
      io.flagOut.c := io.b(7)
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.rrc) {
      io.out := Cat(io.a(0), io.a(7, 1))
      io.flagOut.c := io.b(0)
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.rl) {
      io.out := Cat(io.a(6, 0), io.flagIn.c)
      io.flagOut.c := io.b(7)
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.rr) {
      io.out := Cat(io.flagIn.c, io.a(7, 1))
      io.flagOut.c := io.b(0)
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.sla) {
      io.out := Cat(io.b(6, 0), 0.U)
      io.flagOut.c := io.b(7)
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.sra) {
      io.out := Cat(io.b(7), io.b(7, 1))
      io.flagOut.c := io.b(0)
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.swap) {
      io.out := Cat(io.b(3, 0), io.b(7, 4))
      io.flagOut.c := 0.U
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.srl) {
      io.out := Cat(0.U, io.b(7, 1))
      io.flagOut.c := io.b(0)
      io.flagOut.h := 0.U
      io.flagOut.n := 0.U
      io.flagOut.z := io.out === 0.U
    }
    is(Alu.Opcode.bit) {
      io.out := io.b
      io.flagOut.h := 1.U
      io.flagOut.n := 0.U
      io.flagOut.z := !io.b(io.bitIndex)
    }
    is(Alu.Opcode.set) {
      val bits = VecInit(io.b)
      bits(io.bitIndex) := 1.U
      io.out := bits.asUInt
    }
    is(Alu.Opcode.res) {
      val bits = VecInit(io.b)
      bits(io.bitIndex) := 0.U
      io.out := bits.asUInt
    }
  }
}