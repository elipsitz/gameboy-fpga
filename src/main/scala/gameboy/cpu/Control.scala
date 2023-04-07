package gameboy.cpu

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum

import scala.io.Source

object Microbranch extends ChiselEnum {
  val next, jump, cond, dispatch = Value
}
object DispatchPrefix extends ChiselEnum {
  val none = Value(0.U)
  val cb = Value(1.U)
}
object ImeUpdate extends ChiselEnum {
  val same, enable, disable = Value
}

/** Control signal: how PC should be updated on an M-cycle. */
object PcNext extends ChiselEnum {
  /** Do not change PC. */
  val same = Value
  /** PC = output of incrementer/decrementer */
  val incOut = Value
  /** PC = computed reset address */
  val rstAddr = Value
}

/** Control signal: 8-bit register select. */
object RegSel extends ChiselEnum {
  /** The accumulator register. */
  val a = Value
  /** The flags register. */
  val f = Value
  /** Register C. */
  val c = Value
  /** Temp register W. */
  val w = Value
  /** Temp register Z. */
  val z = Value
  /** Register H. */
  val h = Value
  /** Register L. */
  val l = Value
  /** The 8-bit register denoted by bits 2:0. */
  val reg8Src = Value
  /** The 8-bit register denoted by bits 5:3. */
  val reg8Dest = Value
  /** The high part of the 16-bit register denoted by bits 5:4. */
  val reg16Hi = Value
  /** The low part of the 16-bit register denoted by bits 5:4. */
  val reg16Lo = Value
  /** High byte of SP register. */
  val spHi = Value
  /** Low byte of SP register. */
  val spLo = Value
  /** High byte of PC register. */
  val pcHi = Value
  /** Low byte of PC register. */
  val pcLo = Value
}

/** Control signal: register write operation. */
object RegOp extends ChiselEnum {
  /** Do not write a register. */
  val none = Value
  /** Write the output of the ALU. */
  val writeAlu = Value
  /** Write the memory data input. */
  val writeMem = Value
}

/** Control signal: register incrementer operation. */
object IncOp extends ChiselEnum {
  /** Do nothing. */
  val none = Value
  /** Increment. */
  val inc = Value
  /** Decrement. */
  val dec = Value
  /** Increment but no write-back. */
  val incNoWrite = Value
}

/** Control signal: register incrementer target selector. */
object IncReg extends ChiselEnum {
  /** PC register */
  val pc = Value
  /** HL register */
  val hl = Value
  /** SP register */
  val sp = Value
  /** WZ register */
  val wz = Value
  /** 16-bit register selected by instruction register */
  val inst16 = Value
  /** Target: PC register, Input: {ALU Out, PC Lo} (used for relative jump). */
  val pcAlu = Value
}

/** Control signal: ALU operation. */
object AluOp extends ChiselEnum {
  /** Output = A */
  val copyA = Value
  /** Output = B */
  val copyB = Value
  /** Output = B + 1 */
  val incB = Value
  /** Output = B - 1 */
  val decB = Value
  /** Output = A + B (and set the internal carry flag) */
  val addLo = Value
  /** Output = A + B (and use the internal carry flag) */
  val addHi = Value
  /** Use the "ALU opcode" from the instruction (ADD/ADC/SUB/SBC/AND/XOR/OR/CP). */
  val instAlu = Value
  /** Use the "Acc/Flag opcode" from the instruction (RLCA/RRCA/RLA/RRA/DAA/CPL/SCF/CCF). */
  val instAcc = Value
  /** Use the CB-prefixed opcode from the instruction. */
  val instCB = Value
  /** CB-prefixed single bit operations (get/reset/set) */
  val instBit = Value
}

/** Control signal: ALU operand A source. */
object AluSelA extends ChiselEnum {
  /** A = Accumulator register */
  val regA = Value
  /** A = Register Read 1, */
  val reg1 = Value
}

/** Control signal: ALU operand B source. */
object AluSelB extends ChiselEnum {
  /** B = Register Read 2 */
  val reg2 = Value
  /** B = Sign extension of Register Read 2 */
  val signReg2 = Value
}

/** Control signal: ALU flag set mode. */
object AluFlagSet extends ChiselEnum {
  /** F = ---- (no change) */
  val setNone = Value
  /** F = **** (all set) */
  val setAll = Value
  /** F = -*** (all set except zero) */
  val set_NHC = Value
  /** F = 0*** (all set, zero unset) */
  val set0NHC = Value
}

/** Control signal: where the memory load/store address comes from. */
object MemAddrSel extends ChiselEnum {
  /** **INPUT** to the register incrementer (not output). */
  val incrementer = Value
  /** High address: 0xFF00 | (Register 2) */
  val high = Value
}

class Control extends Module {
  val io = IO(new Bundle {
    val tCycle = Input(UInt(2.W))

    /** Current memory data in. */
    val memDataIn = Input(UInt(8.W))
    /** Whether the current condition (in instruction register) is satisfied based on flags. */
    val condition = Input(Bool())

    /** Control signal: how PC should be updated */
    val pcNext = Output(PcNext())
    /** Control signal: if the instruction reg. should be loaded with memory read */
    val instLoad = Output(Bool())
    /** Control signal: the first register we're reading */
    val regRead1Sel = Output(RegSel())
    /** Control signal: the second register we're reading */
    val regRead2Sel = Output(RegSel())
    /** Control signal: the register we're (maybe) writing to */
    val regWriteSel = Output(RegSel())
    /** Control signal: register write operation to perform */
    val regOp = Output(RegOp())
    /** Control signal: incrementer/decrementer operation to perform */
    val incOp = Output(IncOp())
    /** Control signal: incrementer/decrementer register target */
    val incReg = Output(IncReg())
    /** Control signal: ALU operation */
    val aluOp = Output(AluOp())
    /** Control signal: ALU select A */
    val aluSelA = Output(AluSelA())
    /** Control signal: ALU select B */
    val aluSelB = Output(AluSelB())
    /** Control signal: whether ALU should update flags */
    val aluFlagSet = Output(AluFlagSet())
    /** Control signal: whether we're accessing memory */
    val memEnable = Output(Bool())
    /** Control signal: whether we're writing to memory (if `mem_enable`). */
    val memWrite = Output(Bool())
    /** Control signal: where the memory address comes from */
    val memAddrSel = Output(MemAddrSel())
  })

  val microcode = new Microcode(Source.fromResource("microcode.csv"))

  val state = RegInit(0.U(microcode.stateWidth()))
  val ime = RegInit(false.B)

  // Control signals
  val dispatchPrefix = Wire(DispatchPrefix())
  val microOp = Wire(Microbranch())
  val nextState = Wire(UInt(microcode.stateWidth()))
  val imeUpdate = Wire(ImeUpdate())
  imeUpdate := DontCare
  dispatchPrefix := DontCare
  microOp := DontCare
  nextState := DontCare
  io.pcNext := DontCare
  io.instLoad := DontCare
  io.regRead1Sel := DontCare
  io.regRead2Sel := DontCare
  io.regWriteSel := DontCare
  io.regOp := DontCare
  io.incOp := DontCare
  io.incReg := DontCare
  io.aluOp := DontCare
  io.aluSelA := DontCare
  io.aluSelB := DontCare
  io.aluFlagSet := DontCare
  io.memEnable := DontCare
  io.memWrite := DontCare
  io.memAddrSel := DontCare
  for ((e, index) <- microcode.entries.zipWithIndex) {
    when (state === index.U) {
      microOp := e.microOp
      e.nextState().foreach(nextState := _.U)
      e.dispatchPrefix.foreach(dispatchPrefix := _)
      e.imeUpdate.foreach(imeUpdate := _)
      e.pcNext.foreach(io.pcNext := _)
      e.instLoad.foreach(io.instLoad := _.B)
      e.regRead1Sel.foreach(io.regRead1Sel := _)
      e.regRead2Sel.foreach(io.regRead2Sel := _)
      e.regWriteSel.foreach(io.regWriteSel := _)
      e.regOp.foreach(io.regOp := _)
      e.incOp.foreach(io.incOp := _)
      e.incReg.foreach(io.incReg := _)
      e.aluOp.foreach(io.aluOp := _)
      e.aluSelA.foreach(io.aluSelA := _)
      e.aluSelB.foreach(io.aluSelB := _)
      e.aluFlagSet.foreach(io.aluFlagSet := _)
      e.memEnable.foreach(io.memEnable := _.B)
      e.memWrite.foreach(io.memWrite := _.B)
      e.memAddrSel.foreach(io.memAddrSel := _)
    }
  }

  when (io.tCycle === 3.U) {
    // Advance the state machine.
    switch (microOp) {
      is (Microbranch.next) {
        state := state + 1.U
      }
      is (Microbranch.dispatch) {
        val key = Cat(dispatchPrefix.asUInt, io.memDataIn)
        val dispatchTable = microcode.entries.zipWithIndex.flatMap(e => e._1.encoding.map((_, e._2.U)))
        state := Lookup(key, 1.U, dispatchTable)
      }
      is (Microbranch.jump) {
        state := nextState
      }
      is (Microbranch.cond) {
        state := Mux(io.condition, nextState, state + 1.U)
      }
    }

    // Maybe enable/disable IME.
    switch (imeUpdate) {
      // TODO: this is supposed to take effect after the next instruction?
      is (ImeUpdate.enable) { ime <= true.B }
      is (ImeUpdate.disable) { ime <= false.B }
    }
  }
}