#include <iostream>
#include <format>
#include <vector>
#include <cstdio>
#include <cmath>

#include <SDL2/SDL.h>

#include "audio.hpp"
#include "cartridge.hpp"
#include "simulator.hpp"
#include "window.hpp"

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
    auto cartridge = std::make_unique<Cartridge>(std::filesystem::path(argv[1]));

    // Initialize SDL.
    SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS | SDL_INIT_AUDIO);
    Window window;
    Audio audio;

    Simulator simulator(std::move(cartridge));

    bool single_step = false;
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
                } else if (key.sym == SDLK_n && command) {
                    paused = true;
                    single_step = true;
                } else if (key.sym == SDLK_r && command) {
                    std::cout << "Resetting..." << std::endl;
                    simulator.reset();
                }
            }
        }

        // Simulate for a frame.
        if (!paused || single_step) {
            simulator.set_joypad_state(read_joypad_state());
            simulator.simulate_frame();
            frame_counter++;
            // Audio
            std::vector<int16_t>& samples = simulator.getAudioSampleBuffer();
            audio.push(samples.data(), samples.size());
            samples.clear();
        }
        window.update(simulator.getFramebuffer());
        single_step = false;

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