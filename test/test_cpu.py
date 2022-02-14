import cocotb
from cocotb.triggers import Timer
from cocotb.clock import Clock


def get_register(dut, name):
    """Get the register value from the CPU."""
    registers = "BCDEHLFASPWZ"
    if len(name) == 1:
        index = registers.index(name)
        return dut.registers[index].value.integer
    if len(name) == 2:
        hi = dut.registers[registers.index(name[0])].value.integer
        lo = dut.registers[registers.index(name[1])].value.integer
        return (hi << 8) | lo


async def run_program(dut, program, max_steps=1000):
    """
    Run a program, starting at PC=0x0000, until we hit a HALT instruction
    or the max number of cycles.

    Returns a tuple of (work ram, FF00 ram)
    """
    clk = await cocotb.start(Clock(dut.clk, 1, "ns").start())
    dut.reset.value = 1
    dut.mem_data_in.value = 0
    await Timer(4, units="ns")
    dut.reset.value = 0

    memory = [0] * (8 * 1024)
    high_memory = [0] * 256
    steps = 0

    # Execute until we hit a HALT instruction.
    while True:
        await Timer(2, units="ns")

        instruction = dut.instruction_register.value.integer
        if instruction == 0x76:
            # HALT -- exit.
            break
        if dut.control.state.value.integer == 1:
            # Hit "INVALID" control state.
            raise Exception(f"Hit INVALID state, instruction={instruction:02X}")
        if steps >= max_steps:
            raise Exception("Execution hit max steps")
        # dut._log.info(
        #    f"pc {dut.pc.value.integer:04X} | inst {dut.instruction_register.value.integer:02X} | state={dut.control.state.value.integer}"
        # )

        if dut.mem_enable.value:
            address = dut.mem_addr.value.integer
            if dut.mem_write.value:
                # Write
                data = dut.mem_data_out.value.integer
                # dut._log.info(f"    mem write at {address:04X} <- {data:02X}   |  ")
                if address >= 0xC000 and address <= 0xDFFF:
                    memory[address - 0xC000] = data
                elif address >= 0xFF00 and address <= 0xFFFF:
                    high_memory[address - 0xFF00] = data
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
                elif address >= 0xFF00 and address <= 0xFFFF:
                    output = high_memory[address - 0xFF00]

                # dut._log.info(f"    mem read at {address:04X} -> {output:02X}   |  ")
                dut.mem_data_in.value = output

        await Timer(2, units="ns")
        steps += 1

    return (memory, high_memory)


@cocotb.test()
async def test_nop(dut):
    """Execute a bunch of NOPs."""
    program = b"\x00\x00\x00\x00\x00\x76"
    await run_program(dut, program)


@cocotb.test()
async def test_sanity(dut):
    """
    Test basic operations (load immediate, compare, jump, jump conditional)
    to make sure that more complex tests can work.
    """
    program = open("sanity_test.gb", "rb").read()
    memory, _ = await run_program(dut, bytes(program))
    assert get_register(dut, "A") == 0x22
    assert get_register(dut, "B") == 0xBB
    assert get_register(dut, "C") == 0xCC
    assert get_register(dut, "D") == 0xDD
    assert get_register(dut, "E") == 0xEE
    assert memory[0] == 0x2B


@cocotb.test()
async def test_load(dut):
    """Load values around between registers and memory."""
    program = open("basic_test.gb", "rb").read()
    memory, _ = await run_program(dut, bytes(program))
    assert get_register(dut, "C") == 0xAB
    assert get_register(dut, "D") == 0x06
    assert get_register(dut, "E") == 0x06
    assert get_register(dut, "A") == 0xA3
    assert memory[0] == 0x50
    assert memory[1] == 0x51
    assert memory[2] == 0x52
    assert memory[3] == 0x53
    assert memory[4] == 0x10
    assert memory[5] == 0x0D
    assert memory[6] == 0x0D
    assert memory[7] == 0x4D
    assert memory[8] == 0x30


@cocotb.test()
async def test_complete(dut):
    """
    Run the 'complete' test.

    At the end, checks the last byte of memory. If it's 0, all tests pass.
    Otherwise, it contains the ID of the failed test.
    """
    program = open("complete_test.gb", "rb").read()
    memory, _ = await run_program(dut, bytes(program))
    test_result = memory[-1]
    if test_result != 0:
        print("#" * 80)
        print(f"Complete Test failed: {test_result}")
        print("#" * 80)
        raise Exception(f"Complete test failed! ID = {test_result}")
