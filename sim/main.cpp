#include <iostream>
#include <fstream>
#include <vector>
#include <cstdio>

#include "simulator.hpp"

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

    Simulator simulator(rom);

    for (size_t frame = 0; frame < 60; frame++) {
        // Simulate for a frame.
        simulator.simulate_frame();
    }
}