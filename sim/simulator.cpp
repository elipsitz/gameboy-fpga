#include <iostream>

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

        this->stepFramebuffer();

        top->clk = 0;
        top->eval();
        top->clk = 1;
        top->eval();

        this->cycles++;
    }
}

void Simulator::stepFramebuffer()
{
    bool vblank = top->ppu_vblank && !prev_vblank;
    if (vblank) {
        framebufferIndex = 0;
    }
    prev_hblank = top->ppu_hblank;
    prev_vblank = top->ppu_vblank;

    if (top->pixel_valid) {
        if (framebufferIndex >= frameBuffer.size() - 4) {
            // TODO: make this a fatal error (framebuffer overrun).
            return;
        }

        uint8_t pixel = top->pixel_out;
        uint32_t color = 0x000000; // RGB
        switch (pixel) {
            case 0: color = 0xfafbf6; break;
            case 1: color = 0xc6b7be; break;
            case 2: color = 0x565a75; break;
            case 3: color = 0x0f0f1b; break;
        }

        frameBuffer[framebufferIndex++] = (color >> 0) & 0xFF;
        frameBuffer[framebufferIndex++] = (color >> 8) & 0xFF;
        frameBuffer[framebufferIndex++] = (color >> 16) & 0xFF;
        frameBuffer[framebufferIndex++] = 0xFF;
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