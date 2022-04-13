#pragma once

#include "Vgameboy.h"

class Simulator {
public:
    Simulator(std::vector<uint8_t> rom);
    ~Simulator();

    void simulate_cycles(uint64_t cycles);
    void simulate_frame();
    void reset();
    std::vector<uint8_t>& getFramebuffer();

private:
    std::vector<uint8_t> rom;
    std::vector<uint8_t> frameBuffer;
    uint64_t cycles = 0;
    Vgameboy* top = nullptr;
};
