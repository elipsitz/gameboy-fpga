package gameboy.cpu

import chisel3._
import chiseltest._
import chiseltest.experimental._
import org.scalatest.freespec.AnyFreeSpec

import java.io.ByteArrayOutputStream
import scala.sys.process._
import scala.language.postfixOps
import scala.util.control.Breaks.{break, breakable}

/** Wrapper of Cpu to expose some internal signals. */
class CpuWrapper extends Cpu {
  val xInstructionRegister = expose(instructionRegister)
  val xControlState = expose(control.state)
  val xRegB = expose(registers(0))
  val xRegC = expose(registers(1))
  val xRegD = expose(registers(2))
  val xRegE = expose(registers(3))
  val xRegA = expose(registers(7))
}

class CpuSpec extends AnyFreeSpec with ChiselScalatestTester {
  private def compileTest(name: String): Array[Byte] = {
    val data = getClass.getResourceAsStream("/cpu_tests/" + name)
    val bos = new ByteArrayOutputStream()
    "rgbasm -o - -" #< data #> "rgblink -o - -" #> bos !< ProcessLogger(_ => ())
    bos.toByteArray
  }

  private def runProgram(dut: CpuWrapper, program: Array[Byte], maxSteps: Int = 1000): (Array[Byte], Array[Byte]) = {
    val memory = new Array[Byte](8 * 1024)
    val highMemory = new Array[Byte](256)
    var steps = 0

    dut.io.memDataIn.poke(0.U)
    breakable {
      while (true) {
        dut.clock.step(2)

        // Check state of the DUT.
        val instruction = dut.xInstructionRegister.peekInt()
        if (instruction == 0x76) {
          // HALT: exit
          break()
        }
        if (dut.xControlState.peekInt() == 1) {
          fail("Hit INVALID state on instruction $instruction")
        }
        if (steps > maxSteps) {
          fail("Hit max steps!")
        }

        // Handle memory
        if (dut.io.memEnable.peekBoolean()) {
          val address = dut.io.memAddress.peekInt().toInt
          if (dut.io.memWrite.peekBoolean()) {
            val data = dut.io.memDataOut.peekInt().toByte
//            println(s"---> Write to $address data $data")
            if (address >= 0xC000 && address <= 0xDFFF) {
              memory.update(address - 0xC000, data)
            } else if (address >= 0xFF02 && data == 0x81) {
              // Serial out
              print(highMemory(0x02).toChar)
            } else if (address >= 0xFF00 && address <= 0xFFFF) {
              highMemory.update(address - 0xFF00, data)
            }
          } else {
            var output = 0
            if (address >= 0 && address <= 0x7FFF) {
              if (address < program.length) {
                output = program(address)
              }
            } else if (address >= 0xC000 && address <= 0xDFFF) {
              output = memory(address - 0xC000)
            } else if (address >= 0xF000 && address <= 0xFFFF) {
              output = highMemory(address - 0xFF00)
            }
//            println(s"---> Read from $address data $output")
            dut.io.memDataIn.poke((output & 0xFF).U)
          }
        }

        dut.clock.step(2)
        steps += 1
      }
    }
    (memory, highMemory)
  }

  "compile" in {
    val program = compileTest("sanity_test.s")
    println(s"Program length = ${program.length}")
  }

  "nop" in {
    test(new CpuWrapper) { dut =>
      runProgram(dut, Array(0x00, 0x00, 0x00, 0x00, 0x00, 0x76))
    }
  }

  "sanity" in {
    test(new CpuWrapper) { dut =>
      runProgram(dut, compileTest("sanity_test.s"))
      dut.xRegA.expect(0x22.U)
      dut.xRegB.expect(0xBB.U)
      dut.xRegC.expect(0xCC.U)
      dut.xRegD.expect(0xDD.U)
      dut.xRegE.expect(0xEE.U)
    }
  }

  "load between registers and memory" in {
    test(new CpuWrapper) { dut =>
      val (memory, _) = runProgram(dut, compileTest("basic_test.s"))
      dut.xRegC.expect(0xAB.U)
      dut.xRegD.expect(0x06.U)
      dut.xRegE.expect(0x06.U)
      dut.xRegA.expect(0xA3.U)
      assert(memory(0) == 0x50)
      assert(memory(1) == 0x51)
      assert(memory(2) == 0x52)
      assert(memory(3) == 0x53)
      assert(memory(4) == 0x10)
      assert(memory(5) == 0x0D)
      assert(memory(6) == 0x0D)
      assert(memory(7) == 0x4D)
      assert(memory(8) == 0x30)
    }
  }

  "complete" in {
    test(new CpuWrapper) { dut =>
      val (memory, _) = runProgram(dut, compileTest("complete_test.s"), maxSteps=2000)
      val resultCode = memory(memory.length - 1)
      assert(resultCode == 0)
    }
  }
}
