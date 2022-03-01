#include <iostream>
#include <fstream>
#include <vector>
#include <cstdio>

#include <verilated.h>
#include <string.h>

#include "lib/tester.h"

#include "Vcpu.h"
#include "Vcpu___024root.h"

Vcpu *top;

vluint64_t main_time = 0;

double sc_time_stamp() {
    return main_time;
}

static size_t instruction_mem_size;
static uint8_t *instruction_mem;

static int num_mem_accesses;
static struct mem_access mem_accesses[16];

/*
 * Called once during startup. The area of memory pointed to by
 * tester_instruction_mem will contain instructions the tester will inject, and
 * should be mapped read-only at addresses [0,tester_instruction_mem_size).
 */
static void mycpu_init(size_t tester_instruction_mem_size,
                       uint8_t *tester_instruction_mem)
{
    instruction_mem_size = tester_instruction_mem_size;
    instruction_mem = tester_instruction_mem;

    top = new Vcpu;

    /* ... Initialize your CPU here ... */
}

/*
 * Resets the CPU state (e.g., registers) to a given state state.
 */
static void mycpu_set_state(struct state *state)
{
    (void)state;

    num_mem_accesses = 0;

    // Do a reset.
    top->reset = 1;
    for (int i = 0; i < 8; i++) {
        top->clk = i % 2;
        top->eval();
    }
    top->reset = 0;

    // Load the state.
    top->rootp->cpu__DOT__registers[0] = state->reg8.B;
    top->rootp->cpu__DOT__registers[1] = state->reg8.C;
    top->rootp->cpu__DOT__registers[2] = state->reg8.D;
    top->rootp->cpu__DOT__registers[3] = state->reg8.E;
    top->rootp->cpu__DOT__registers[4] = state->reg8.H;
    top->rootp->cpu__DOT__registers[5] = state->reg8.L;
    top->rootp->cpu__DOT__registers[6] = state->reg8.F;
    top->rootp->cpu__DOT__registers[7] = state->reg8.A;
    top->rootp->cpu__DOT__control__DOT__ime = state->interrupts_master_enabled;
    top->rootp->cpu__DOT__registers[12] = (state->PC & 0xFF00) >> 8;
    top->rootp->cpu__DOT__registers[13] = state->PC & 0xFF;
    top->rootp->cpu__DOT__registers[8] = (state->SP & 0xFF00) >> 8;
    top->rootp->cpu__DOT__registers[9] = state->SP & 0xFF;
}

/*
 * Query the current state of the CPU.
 */
static void mycpu_get_state(struct state *state)
{
    state->num_mem_accesses = num_mem_accesses;
    memcpy(state->mem_accesses, mem_accesses, sizeof(mem_accesses));

    /* ... Copy your current CPU state into the provided struct ... */
    state->reg8.B = top->rootp->cpu__DOT__registers[0];
    state->reg8.C = top->rootp->cpu__DOT__registers[1];
    state->reg8.D = top->rootp->cpu__DOT__registers[2];
    state->reg8.E = top->rootp->cpu__DOT__registers[3];
    state->reg8.H = top->rootp->cpu__DOT__registers[4];
    state->reg8.L = top->rootp->cpu__DOT__registers[5];
    state->reg8.F = top->rootp->cpu__DOT__registers[6];
    state->reg8.A = top->rootp->cpu__DOT__registers[7];
    state->interrupts_master_enabled = top->rootp->cpu__DOT__control__DOT__ime;
    state->PC = (top->rootp->cpu__DOT__registers[12] << 8) | top->rootp->cpu__DOT__registers[13];
    state->SP = (top->rootp->cpu__DOT__registers[8] << 8) | top->rootp->cpu__DOT__registers[9];
    state->halted = top->rootp->cpu__DOT__control__DOT__state == 2;

    state->PC -= 1;
}

/*
 * Example mock MMU implementation, mapping the tester's instruction memory
 * read-only at address 0, and logging all writes.
 */

uint8_t mymmu_read(uint16_t address)
{
    if (address < instruction_mem_size)
        return instruction_mem[address];
    else
        return 0xaa;
}

void mymmu_write(uint16_t address, uint8_t data)
{
    struct mem_access *access = &mem_accesses[num_mem_accesses++];
    access->type = MEM_ACCESS_WRITE;
    access->addr = address;
    access->val = data;
}

/*
 * Step a single instruction of the CPU. Returns the amount of cycles this took
 * (e.g., NOP should return 4).
 */
static int mycpu_step(void)
{
    int steps = 0;

    /* ... Execute a single instruction in your CPU ... */
    while (steps < 80) {
        steps += 1;

        bool new_instruction = 
            // microbranch = dispatch
            (top->rootp->cpu__DOT__control__DOT__microbranch == 3) &&
            (top->rootp->cpu__DOT__control__DOT__dispatch_prefix == 0) &&
            (top->rootp->cpu__DOT__t_cycle == 3)
            ;
        if (new_instruction && steps > 9) {
            top->clk = steps % 2;
            top->eval();
            steps += 1;
            top->clk = steps % 2;
            top->eval();

            break;
        }

        if (steps % 8 == 4) {
            if (top->mem_enable) {
                uint16_t address = top->mem_addr;
                if (top->mem_write) {
                    uint8_t data = top->mem_data_out;
                    mymmu_write(address, data);
                } else {
                    top->mem_data_in = mymmu_read(address);
                }
            }
        }

        top->clk = steps % 2;
        top->eval();
    }

    return steps / 8;
}

struct tester_operations myops = {
    .init = mycpu_init,
    .set_state = mycpu_set_state,
    .get_state = mycpu_get_state,
    .step = mycpu_step,
};






static struct tester_flags flags = {
    .keep_going_on_mismatch = 1,
    .enable_cb_instruction_testing = 1,
    .print_tested_instruction = 1,
    .print_verbose_inputs = 0,
};

int main(int argc, char** argv) {
    tester_run(&flags, &myops);
}
