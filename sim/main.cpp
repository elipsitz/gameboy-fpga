#include <iostream>
#include <fstream>
#include <vector>
#include <cstdio>

#include <SDL2/SDL.h>

#include "simulator.hpp"

const uint64_t FRAME_MS = 1000 / 16;

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

    uint64_t last_frame = 0;
    while (true) {
        // Handle SDL events.
        SDL_Event event;
        if (SDL_PollEvent(&event)) {
            if (event.type == SDL_QUIT) {
                break;
            }
        }

        // Simulate for a frame.
        simulator.simulate_frame();

        // Sleep for the rest of the frame.
        // TODO: vsync instead
        uint64_t ticks = SDL_GetTicks64();
        if (last_frame + FRAME_MS > ticks) {
            SDL_Delay((uint32_t)(FRAME_MS - (ticks - last_frame)));
        }
        last_frame = ticks;
    }
}