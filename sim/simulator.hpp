#pragma once

#include "VGameboy.h"

struct JoypadState {
    bool start;
    bool select;
    bool b;
    bool a;
    bool down;
    bool up;
    bool left;
    bool right;
};

class Simulator {
public:
    Simulator(std::vector<uint8_t> rom);
    ~Simulator();

    void set_joypad_state(JoypadState state);
    void simulate_cycles(uint64_t cycles);
    void simulate_frame();
    void reset();
    std::vector<uint8_t>& getFramebuffer();

private:
    void stepFramebuffer();

    std::vector<uint8_t> rom;
    std::vector<uint8_t> frameBuffer;
    uint64_t cycles = 0;
    VGameboy* top = nullptr;
    size_t framebufferIndex = 0;
    bool prev_vblank = false;
    bool prev_hblank = false;
};
