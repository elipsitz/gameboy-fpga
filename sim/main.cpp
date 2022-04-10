#include <iostream>
#include <fstream>
#include <vector>
#include <cstdio>

#include <verilated.h>

#include "Vgameboy.h"

Vgameboy *top;

vluint64_t main_time = 0;

double sc_time_stamp() {
    return main_time;
}

std::vector<uint8_t> read_file(const char* path) {
    std::vector<uint8_t> buffer;
    std::ifstream in(path, std::ios::binary);
    in.seekg(0, std::ios::end);
    size_t size = in.tellg();
    in.seekg(0, std::ios::beg);
    buffer.resize(size);
    in.read(reinterpret_cast<char*>(buffer.data()), size);
    return buffer;
}


int main(int argc, char** argv) {
    if (argc != 2) {
        std::cout << "Usage: sim [rom.gb]" << std::endl;
        return 1;
    }
    auto rom = read_file(argv[1]);
    uint64_t max_time = 8 * 1000000 * 20;

    top = new Vgameboy;
    top->reset = 1;
    top->cart_data_in = 0;

    while (!Verilated::gotFinish() && main_time < max_time) {
        if (main_time > 8) {
            // Deassert reset.
            top->reset = 0;
        }
        if (main_time % 8000000 == 0) {
            // printf("Time: %llu\n", main_time);
        }
        if (main_time % 8 == 4) {
            // Handle memory.
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

        top->clk = main_time % 2;
        top->eval();
        main_time++;
    }

    top->final();
    delete top;
}