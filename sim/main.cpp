#include <iostream>
#include <fstream>
#include <format>
#include <vector>
#include <cstdio>
#include <cmath>

#include <SDL2/SDL.h>

#include "audio.hpp"
#include "simulator.hpp"
#include "window.hpp"

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

JoypadState read_joypad_state() {
    const uint8_t* keyboard = SDL_GetKeyboardState(nullptr);
    JoypadState joypad = {};
    joypad.start = keyboard[SDL_SCANCODE_RETURN];
    joypad.select = keyboard[SDL_SCANCODE_RSHIFT];
    joypad.b = keyboard[SDL_SCANCODE_X];
    joypad.a = keyboard[SDL_SCANCODE_Z];
    joypad.down = keyboard[SDL_SCANCODE_DOWN];
    joypad.up = keyboard[SDL_SCANCODE_UP];
    joypad.left = keyboard[SDL_SCANCODE_LEFT];
    joypad.right = keyboard[SDL_SCANCODE_RIGHT];
    return joypad;
}

int main(int argc, char** argv) {
    if (argc != 2) {
        std::cout << "Usage: sim [rom.gb]" << std::endl;
        return 1;
    }
    auto rom = read_file(argv[1]);

    // Initialize SDL.
    SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS | SDL_INIT_AUDIO);
    Window window;
    Audio audio;

    Simulator simulator(rom);

    bool paused = false;
    uint64_t frame_timer = 0;
    int frame_counter = 0;
    while (true) {
        // Handle SDL events.
        SDL_Event event;
        if (SDL_PollEvent(&event)) {
            if (event.type == SDL_QUIT) {
                break;
            } else if (event.type == SDL_KEYDOWN) {
                auto key = event.key.keysym;
                bool command = (key.mod & KMOD_GUI) != 0;
                if (key.sym == SDLK_p && command) {
                    paused = !paused;
                }
            }
        }

        // Simulate for a frame.
        if (!paused) {
            simulator.set_joypad_state(read_joypad_state());
            simulator.simulate_frame();
            frame_counter++;
        }
        // audio.push(samples.data(), samples.size());
        window.update(simulator.getFramebuffer());

        // Update title.
        if (SDL_GetTicks64() - frame_timer >= 1000) {
            char buffer[100];
            snprintf(buffer, 100, "Game Boy - FPS: %d", frame_counter);
            window.setTitle(buffer);
            frame_counter = 0;
            frame_timer = SDL_GetTicks64();
        }
    }

    SDL_Quit();
}