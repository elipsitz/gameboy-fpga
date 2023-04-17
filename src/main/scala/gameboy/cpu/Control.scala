package gameboy.cpu

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.util._
import chisel3.experimental.{ChiselAnnotation, annotate}
import firrtl.annotations.MemoryArrayInitAnnotation

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
  /** PC = computed interrupt address */
  val interrupt = Value
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

class MemROM(w: Int, contents: Seq[BigInt]) extends Module {
  val abits = log2Ceil(contents.length)
  val io = IO(new Bundle {
    val addr = Input(UInt(abits.W))
    val data = Output(UInt(w.W))
  })

  val mem = Mem(contents.length, chiselTypeOf(io.data))
  annotate(new ChiselAnnotation {
    override def toFirrtl = MemoryArrayInitAnnotation(mem.toTarget, contents)
  })
  io.data := mem.read(io.addr)
}

class Control extends Module {
  val io = IO(new Bundle {
    val tCycle = Input(UInt(2.W))

    /** Current memory data in. */
    val memDataIn = Input(UInt(8.W))
    /** Whether the current condition (in instruction register) is satisfied based on flags. */
    val condition = Input(Bool())
    /** Whether interrupts are pending (IE & IF) =/= 0 */
    val interruptsPending = Input(Bool())

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

  class MicrocodeRow extends Bundle {
    val microOp = Microbranch()
    val nextState = UInt(microcode.stateWidth())
    val dispatchPrefix = DispatchPrefix()
    val imeUpdate = ImeUpdate()
    val pcNext = PcNext()
    val instLoad = Bool()
    val regRead1Sel = RegSel()
    val regRead2Sel = RegSel()
    val regWriteSel = RegSel()
    val regOp = RegOp()
    val incOp = IncOp()
    val incReg = IncReg()
    val aluOp = AluOp()
    val aluSelA = AluSelA()
    val aluSelB = AluSelB()
    val aluFlagSet = AluFlagSet()
    val memEnable = Bool()
    val memWrite = Bool()
    val memAddrSel = MemAddrSel()
  }

  val state = RegInit(0.U(microcode.stateWidth()))
  val ime = RegInit(false.B)

  val controlTable = Module(new MemROM(
    (new MicrocodeRow).getWidth,
    microcode.entries.map(e => {
      val row = (new MicrocodeRow).Lit(
        _.microOp -> e.microOp,
        _.nextState -> e.nextState().getOrElse(0).U,
        _.dispatchPrefix -> e.dispatchPrefix.getOrElse(DispatchPrefix.none),
        _.imeUpdate -> e.imeUpdate.getOrElse(ImeUpdate.same),
        _.pcNext -> e.pcNext.getOrElse(PcNext.same),
        _.instLoad -> e.instLoad.getOrElse(false).B,
        _.regRead1Sel -> e.regRead1Sel.getOrElse(RegSel.a),
        _.regRead2Sel -> e.regRead2Sel.getOrElse(RegSel.a),
        _.regWriteSel -> e.regWriteSel.getOrElse(RegSel.a),
        _.regOp -> e.regOp.getOrElse(RegOp.none),
        _.incOp -> e.incOp.getOrElse(IncOp.none),
        _.incReg -> e.incReg.getOrElse(IncReg.pc),
        _.aluOp -> e.aluOp.getOrElse(AluOp.copyA),
        _.aluSelA -> e.aluSelA.getOrElse(AluSelA.regA),
        _.aluSelB -> e.aluSelB.getOrElse(AluSelB.reg2),
        _.aluFlagSet -> e.aluFlagSet.getOrElse(AluFlagSet.setNone),
        _.memEnable -> e.memEnable.getOrElse(false).B,
        _.memWrite -> e.memWrite.getOrElse(false).B,
        _.memAddrSel -> e.memAddrSel.getOrElse(MemAddrSel.incrementer),
      )
      row.litValue
    })
  ))
  controlTable.io.addr := state
  val row = controlTable.io.data.asTypeOf(new MicrocodeRow)

  // Control signals
  val dispatchPrefix = Wire(DispatchPrefix())
  val microOp = Wire(Microbranch())
  val nextState = Wire(UInt(microcode.stateWidth()))
  val imeUpdate = Wire(ImeUpdate())
  imeUpdate := row.imeUpdate
  dispatchPrefix := row.dispatchPrefix
  microOp := row.microOp
  nextState := row.nextState
  io.pcNext := row.pcNext
  io.instLoad := row.instLoad
  io.regRead1Sel := row.regRead1Sel
  io.regRead2Sel := row.regRead2Sel
  io.regWriteSel := row.regWriteSel
  io.regOp := row.regOp
  io.incOp := row.incOp
  io.incReg := row.incReg
  io.aluOp := row.aluOp
  io.aluSelA := row.aluSelA
  io.aluSelB := row.aluSelB
  io.aluFlagSet := row.aluFlagSet
  io.memEnable := row.memEnable
  io.memWrite := row.memWrite
  io.memAddrSel := row.memAddrSel

  val halted = state === microcode.stateForLabel("HALT").U
  val justFetched = RegInit(false.B)
  justFetched := false.B
  when (io.tCycle === 0.U && io.interruptsPending && (justFetched || halted)) {
    // Handle interrupts.
    when (ime) {
      // Note: IME is unset in microcode
      state := microcode.stateForLabel("#Interrupt").U
    }
    when (halted) {
      state := microcode.stateForLabel("NOP").U
    }
    // TODO exit HALT
  }

  when (io.tCycle === 3.U) {
    // Advance the state machine.
    switch (microOp) {
      is (Microbranch.next) {
        state := state + 1.U
      }
      is (Microbranch.dispatch) {
        val dispatchTable = Module(new MemROM(
          microcode.stateWidth().get,
          (0 until 512).map(i =>
            microcode.entries.zipWithIndex
              .flatMap(e => e._1.encoding.map((_, e._2)))
              .find(e => e._1.value == (i & e._1.mask))
              .map(e => BigInt(e._2))
              .getOrElse(BigInt(microcode.stateForLabel("_INVALID")))
          )
        ))
        val dispatchKey = Cat(dispatchPrefix.asUInt, io.memDataIn)
        dispatchTable.io.addr := dispatchKey
        state := dispatchTable.io.data
        justFetched := dispatchPrefix === DispatchPrefix.none
      }
      is (Microbranch.jump) {
        state := nextState
      }
      is (Microbranch.cond) {
        state := Mux(io.condition, nextState, state + 1.U)
      }
    }

    // Maybe enable/disable IME.
    val delayedImeSet = RegInit(false.B)
    when (io.tCycle === 3.U) {
      when(delayedImeSet) {
        ime := true.B
        delayedImeSet := false.B
      }
      when(imeUpdate === ImeUpdate.enable) { delayedImeSet := true.B }
      when(imeUpdate === ImeUpdate.disable) { ime := false.B}
    }
  }
}