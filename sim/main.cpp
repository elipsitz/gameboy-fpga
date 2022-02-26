#include <iostream>
#include <fstream>
#include <vector>
#include <cstdio>

#include <verilated.h>

#include "Vcpu.h"

Vcpu *top;

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

    uint8_t memory[8192] = {0};
    uint8_t high_memory[256] = {0};

    top = new Vcpu;
    top->reset = 1;
    top->mem_data_in = 0;

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
            if (top->mem_enable) {
                uint16_t address = top->mem_addr;
                if (top->mem_write) {
                    uint8_t data = top->mem_data_out;
                    // printf("mem write at [%.04X] <= [%.02X]\n", address, data);
                    if (address >= 0xC000 && address <= 0xDFFF) {
                        memory[address - 0xC000] = data;
                    } else if (address == 0xFF02 && data == 0x81) {
                        // Serial Write
                        char c = high_memory[0x01];
                        printf("%c", c); 
                    } else if (address >= 0xFF00 && address <= 0xFFFF) {
                        high_memory[address - 0xFF00] = data;
                    }
                } else {
                    uint8_t data = 0;
                    if (address >= 0x0000 && address <= 0x7FFF) {
                        data = rom[address];
                    } else if (address >= 0xC000 && address <= 0xDFFF) {
                        data = memory[address - 0xC000];
                    } else if (address >= 0xFF00 && address <= 0xFFFF) {
                        data = high_memory[address - 0xFF00];
                    }
                    // printf("mem read at [%.04X] => [%.02X]\n", address, data);
                    top->mem_data_in = data;
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