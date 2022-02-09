import cocotb
from cocotb.triggers import Timer
from cocotb.clock import Clock

async def run_program(dut, program, max_steps=10):
    clk = await cocotb.start(Clock(dut.clk, 1, "ns").start())
    dut.reset.value = 1
    dut.mem_data_in.value = 0
    await Timer(4, units="ns")
    dut.reset.value = 0

    memory = [0] * (8 * 1024)
    steps = 0

    # Execute until PC hits the end of the program.
    while dut.pc.value.integer < len(program):
        await Timer(2, units="ns")

        if steps >= max_steps:
            raise Exception("Execution hit max steps")
        dut._log.info(f"pc {dut.pc.value.integer:04X} | inst {dut.instruction_register.value.integer:02X} | state={dut.control.state.value.integer} | t={dut.t_cycle.value.integer}")

        if dut.mem_enable.value:
            address = dut.mem_addr.value.integer
            if dut.mem_write.value:
                # Write
                data = dut.mem_data_out.value.integer
                if address >= 0xC000 and address <= 0xDFFF:
                    memory[address - 0xC000] = data
            else:
                # Read
                output = 0
                if address >= 0 and address <= 0x7FFF:
                    # ROM read
                    if address < len(program):
                        output = program[address]
                elif address >= 0xC000 and address <= 0xDFFF:
                    # RAM read
                    output = memory[address - 0xC000]

                dut._log.info(f"    mem read at {address:04X} -> {output:02X}   |  ")
                dut.mem_data_in.value = output

        await Timer(2, units="ns")
        steps += 1


@cocotb.test()
async def nop_slide(dut):
    """Execute a bunch of NOPs."""
    program = b"\x00\x00\x00\x00\x00"
    await run_program(dut, program)

@cocotb.test()
async def reg_load(dut):
    """Execute ld b, 0xAB."""
    program = b"\x06\xAB\x00"
    await run_program(dut, program)
    assert dut.registers[0].value == 0xAB
        
