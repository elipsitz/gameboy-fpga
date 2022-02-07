import cocotb
from cocotb.triggers import Timer
from cocotb.clock import Clock


@cocotb.test()
async def clocking(dut):
    """Basic clocking."""
    clk = await cocotb.start(Clock(dut.clk, 1, "ns").start())
    dut.reset.value = 0

    for cycle in range(16):
        dut._log.info(f"pc is {dut.pc.value.integer:04X}")
        await Timer(4, units="ns")
        
