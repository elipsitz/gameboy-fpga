package gameboy.cpu

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class CpuSpec extends AnyFreeSpec with ChiselScalatestTester {
  "basic test" in {
    test(new Cpu) { dut =>
    }
  }
}
