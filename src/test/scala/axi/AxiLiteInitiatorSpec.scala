package axi

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec

class AxiLiteInitiatorSpec extends AnyFreeSpec with ChiselScalatestTester {
  private def setup(dut: AxiLiteInitiator): Unit = {
    dut.io.enable.poke(false)
    dut.io.read.poke(true)
    dut.io.signals.arready.poke(false)
    dut.io.signals.rvalid.poke(false)
    dut.io.signals.awready.poke(false)
    dut.io.signals.wready.poke(false)
    dut.io.signals.bvalid.poke(false)
  }


  "read" in {
    test(new AxiLiteInitiator(32)) { dut =>
      setup(dut)
      dut.io.signals.arvalid.expect(false)
      dut.io.busy.expect(false)

      dut.clock.step()
      dut.io.signals.arvalid.expect(false)
      dut.io.busy.expect(false)

      dut.io.enable.poke(true)
      dut.io.read.poke(true)
      dut.io.address.poke(0x1230)
      dut.io.signals.arvalid.expect(true)

      for (i <- 0 to 2) {
        dut.clock.step()
        dut.io.enable.poke(false)
        dut.io.address.poke(0x1000 + i)
        dut.io.busy.expect(true)
        dut.io.signals.arvalid.expect(true)
      }

      dut.io.signals.arready.poke(true)
      dut.io.signals.araddr.expect(0x1230)
      dut.clock.step()

      dut.io.signals.arready.poke(false)
      dut.io.signals.arvalid.expect(false)
      dut.io.busy.expect(true)

      for (_ <- 0 to 10) {
        dut.clock.step()
        dut.io.busy.expect(true)
        dut.io.signals.arvalid.expect(false)
        dut.io.signals.rready.expect(true)
      }

      dut.io.signals.rdata.poke(0xAABBCC)
      dut.io.signals.rvalid.poke(true)
      dut.clock.step()

      dut.io.signals.rvalid.poke(false)
      dut.io.busy.expect(false)
      dut.io.readData.expect(0xAABBCC)
    }
  }
}
