import cocotb
from cocotb.triggers import Timer
from cocotb.clock import Clock


@cocotb.test()
async def clocking(dut):
    """Basic clocking."""
    clk = await cocotb.start(Clock(dut.clk, 1, "ns").start())

    for cycle in range(10):
        dut._log.info("pc is %s", dut.pc.value)
        await Timer(1, units="ns")
        
