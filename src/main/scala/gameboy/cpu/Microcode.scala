package gameboy.cpu

import chisel3._
import chisel3.internal.firrtl.Width
import chisel3.util._
import com.github.tototoshi.csv.CSVReader
import gameboy.cpu.Microcode._

import scala.io.Source
import scala.language.implicitConversions

class Microcode(val source: Source) { self =>
  val entries: Seq[Entry] =
    CSVReader.open(source)
      .iteratorWithHeaders
      .zipWithIndex
      .map(entry => {
        val (row, index) = entry
        val data = collection.mutable.Map() ++ row
        // TODO basic validation? signal keys, and values
        val encoding = Option.unless(data("Encoding").isEmpty) {
          val encoding_len = data("Encoding").length
          val encoding_cb = data("Encoding").startsWith("CB_")
          if (!(encoding_len == 8 || (encoding_len == 11 && encoding_cb))) {
            throw new IllegalArgumentException(s"Row $index: Bad encoding")
          }
          if (encoding_cb) {
            BitPat("b1" + data("Encoding").substring(3))
          } else {
            BitPat("b0" + data("Encoding"))
          }
        }
        if (data("MemWr") == "Yes" && data("AluOp") == "-") {
          throw new RuntimeException("Row $index: AluOp must be set if MemWr is Yes")
        }
        if (data("MemAddrSel") == "Incrementer" && data("IncReg") == "-") {
          throw new RuntimeException("Row $index: IncReg must be set if MemAddrSel is Incrementer")
        }

        data("MicroBranch") match {
          case "Fetch" | "Fetch*" => {
            val forcedProperties = Map(
              "PcNext" -> "IncOut",
              "MemEn" -> "Yes",
              "MemWr" -> "No",
              "MemAddrSel" -> "Incrementer",
              "IncOp" -> "Inc",
              "IncReg" -> "PC",
              "NextState" -> "-",
              "InstLoad" -> "Yes",
              "DispatchPrefix" -> "None",
            )
            for ((k, v) <- forcedProperties) {
              if (row.getOrElse(k, "-") == "-") {
                data.update(k, v)
              } else if (row("MicroBranch") == "Fetch") {
                throw new RuntimeException("Row $index: $k must be '-' w/ Fetch")
              }
            }
          }
          case "DispatchCB" => {
            data.update("InstLoad", "Yes")
            data.update("DispatchPrefix", "CB")
          }
          case _ => {
            data.update("InstLoad", "No")
            data.update("DispatchPrefix", "-")
          }
        }

        Entry(
          label = data("Label"),
          microOp = lookup(mapMicroBranch, data("MicroBranch")).get,
          encoding = encoding,
          nextStateLabel = data("NextState"),
          dispatchPrefix = lookup(mapDispatchPrefix, data("DispatchPrefix")),
          pcNext = lookup(mapPcNext, data("PcNext")),
          instLoad = mapBool(data("InstLoad")),
          regRead1Sel = lookup(mapRegSelect, data("RegRead1Sel")),
          regRead2Sel = lookup(mapRegSelect, data("RegRead2Sel")),
          regWriteSel = lookup(mapRegSelect, data("RegWriteSel")),
          regOp = lookup(mapRegOp, data("RegOp")),
          incOp = lookup(mapIncOp, data("IncOp")),
          incReg = lookup(mapIncReg, data("IncReg")),
          aluOp = lookup(mapAluOp, data("AluOp")),
          aluSelA = lookup(mapAluSelA, data("AluSelA")),
          aluSelB = lookup(mapAluSelB, data("AluSelB")),
          aluFlagSet = lookup(mapAluFlagSet, data("AluFlagSet")),
          memEnable = mapBool(data("MemEn")),
          memWrite = mapBool(data("MemWr")),
          memAddrSel = lookup(mapMemAddrSel, data("MemAddrSel")),
          imeUpdate = lookup(mapIME, data("IME")),
        )
      })
      .toSeq

//  println(entries)
  def numStates(): Int = entries.length
  def stateWidth(): Width = math.ceil(math.log(numStates()) / math.log(2)).toInt.W

  def stateForLabel(label: String): Int = {
    self.entries.zipWithIndex.find(_._1.label == label).get._2
  }

  case class Entry
  (
    // Microcode
    label: String,
    microOp: Microbranch.Type,
    encoding: Option[BitPat],
    nextStateLabel: String, // TODO
    dispatchPrefix: Option[DispatchPrefix.Type],
    // Control signals
    pcNext: Option[PcNext.Type],
    instLoad: Option[Boolean],
    regRead1Sel: Option[RegSel.Type],
    regRead2Sel: Option[RegSel.Type],
    regWriteSel: Option[RegSel.Type],
    regOp: Option[RegOp.Type],
    incOp: Option[IncOp.Type],
    incReg: Option[IncReg.Type],
    aluOp: Option[AluOp.Type],
    aluSelA: Option[AluSelA.Type],
    aluSelB: Option[AluSelB.Type],
    aluFlagSet: Option[AluFlagSet.Type],
    memEnable: Option[Boolean],
    memWrite: Option[Boolean],
    memAddrSel: Option[MemAddrSel.Type],
    imeUpdate: Option[ImeUpdate.Type],
  ) {
    def nextState(): Option[Int] = {
      Option.unless(nextStateLabel == "-") {
        stateForLabel(nextStateLabel)
      }
    }
  }
}

object Microcode {
  private def lookup[T](mapping: Map[String, T], key: String): Option[T] = {
    Option.unless(key == "-")(mapping(key))
  }

  private def mapBool(key: String): Option[Boolean] = {
    key match {
      case "Yes" => Option(true)
      case "No" => Option(false)
      case "-" => None
      case _ => throw new IllegalArgumentException()
    }
  }

  private val mapRegSelect = Map(
    "A" -> RegSel.a,
    "F" -> RegSel.f,
    "C" -> RegSel.c,
    "W" -> RegSel.w,
    "Z" -> RegSel.z,
    "H" -> RegSel.h,
    "L" -> RegSel.l,
    "Reg8Src" -> RegSel.reg8Src,
    "Reg8Dest" -> RegSel.reg8Dest,
    "Reg16Hi" -> RegSel.reg16Hi,
    "Reg16Lo" -> RegSel.reg16Lo,
    "SP_Hi" -> RegSel.spHi,
    "SP_Lo" -> RegSel.spLo,
    "PC_Hi" -> RegSel.pcHi,
    "PC_Lo" -> RegSel.pcLo,
  )
  private val mapPcNext = Map(
    "Same" -> PcNext.same,
    "IncOut" -> PcNext.incOut,
    "RstAddr" -> PcNext.rstAddr,
    "Interrupt" -> PcNext.interrupt,
  )
  private val mapRegOp = Map(
    "None" -> RegOp.none,
    "WriteAlu" -> RegOp.writeAlu,
    "WriteMem" -> RegOp.writeMem,
  )
  private val mapIncOp = Map(
    "No" -> IncOp.none,
    "Inc" -> IncOp.inc,
    "Dec" -> IncOp.dec,
    "IncNoWrite" -> IncOp.incNoWrite,
  )
  private val mapIncReg = Map(
    "PC" -> IncReg.pc,
    "HL" -> IncReg.hl,
    "SP" -> IncReg.sp,
    "WZ" -> IncReg.wz,
    "Inst16" -> IncReg.inst16,
    "PC_ALU" -> IncReg.pcAlu,
  )
  private val mapAluOp = Map(
    "CopyA" -> AluOp.copyA,
    "CopyB" -> AluOp.copyB,
    "IncB" -> AluOp.incB,
    "DecB" -> AluOp.decB,
    "InstAlu" -> AluOp.instAlu,
    "InstAcc" -> AluOp.instAcc,
    "InstCB" -> AluOp.instCB,
    "InstBit" -> AluOp.instBit,
    "AddLo" -> AluOp.addLo,
    "AddHi" -> AluOp.addHi,
  )
  private val mapAluSelA = Map(
    "A" -> AluSelA.regA,
    "Reg1" -> AluSelA.reg1,
  )
  private val mapAluSelB = Map(
    "Reg2" -> AluSelB.reg2,
    "SignReg2" -> AluSelB.signReg2,
  )
  private val mapAluFlagSet = Map(
    "No" -> AluFlagSet.setNone,
    "All" -> AluFlagSet.setAll,
    "-***" -> AluFlagSet.set_NHC,
    "0***" -> AluFlagSet.set0NHC,
  )
  private val mapMemAddrSel = Map(
    "Incrementer" -> MemAddrSel.incrementer,
    "High" -> MemAddrSel.high,
  )
  private val mapMicroBranch = Map(
    "Next" -> Microbranch.next,
    "Jump" -> Microbranch.jump,
    "Cond" -> Microbranch.cond,
    "Fetch" -> Microbranch.dispatch,
    "Fetch*" -> Microbranch.dispatch,
    "DispatchCB" -> Microbranch.dispatch,
  )
  private val mapDispatchPrefix = Map(
    "None" -> DispatchPrefix.none,
    "CB" -> DispatchPrefix.cb,
  )
  private val mapIME = Map(
    "=" -> ImeUpdate.same,
    "En" -> ImeUpdate.enable,
    "Dis" -> ImeUpdate.disable,
  )
}
