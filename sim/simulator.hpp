#pragma once

#include "Vgameboy.h"

class Simulator {
public:
    Simulator(std::vector<uint8_t> rom);
    ~Simulator();

    void simulate_cycles(uint64_t cycles);
    void simulate_frame();
    void reset();

private:
    std::vector<uint8_t> rom;
    uint64_t cycles = 0;
    Vgameboy* top = nullptr;
};
