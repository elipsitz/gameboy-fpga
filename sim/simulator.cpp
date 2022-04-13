#include "common.hpp"
#include "simulator.hpp"

Simulator::Simulator(std::vector<uint8_t> rom)
{
    this->top = new Vgameboy;
    this->rom = rom;
    this->frameBuffer.resize(WIDTH * HEIGHT * 4, 0xFF);

    reset();
}

Simulator::~Simulator()
{
    top->final();
    delete top;
}

void Simulator::reset()
{
    top->cart_data_in = 0;
    top->reset = 1;

    uint64_t total = 4 - (cycles % 1);
    simulate_cycles(total);

    top->reset = 0;
}

void Simulator::simulate_cycles(uint64_t num_cycles)
{
    for (uint64_t i = 0; i < num_cycles; i++) {
        // Handle memory.
        if (this->cycles % 4 == 2) {
            if (top->cart_enable) {
                uint16_t address = top->cart_addr;
                if (top->cart_write) {
                    // uint8_t data = top->cart_data_out;
                    // printf("cart write at [%.04X] <= [%.02X]\n", address, data);
                } else {
                    uint8_t data = 0;
                    if (address >= 0x0000 && address <= 0x7FFF) {
                        data = rom[address];
                    }
                    // printf("mem read at [%.04X] => [%.02X]\n", address, data);
                    top->cart_data_in = data;
                }
            }
        }

        top->clk = 0;
        top->eval();
        top->clk = 1;
        top->eval();

        this->cycles++;
    }
}

void Simulator::simulate_frame()
{
    simulate_cycles(70224);
}

std::vector<uint8_t>& Simulator::getFramebuffer()
{
    return this->frameBuffer;
}