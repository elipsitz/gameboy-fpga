#pragma once

#include <SDL2/SDL.h>

const int AUDIO_SAMPLE_RATE = 256 * 1024;
const int AUDIO_CHANNELS = 2;

class Audio {
public:
    Audio();
    ~Audio();

    // Push samples (up to the maximum limit).
    // Each sample is signed (left, right) pair.
    void push(int16_t* data, uint32_t length);

private:
	SDL_AudioDeviceID device;
};