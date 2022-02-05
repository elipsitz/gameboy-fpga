import cocotb
from cocotb.triggers import Timer
from cocotb.clock import Clock


@cocotb.test()
async def clocking(dut):
    """Basic clocking."""
    clk = await cocotb.start(Clock(dut.clk, 1, "ns").start())

    for cycle in range(16):
        dut._log.info("pc is {:04X}".format(dut.pc.value.integer))
        await Timer(4, units="ns")
        
