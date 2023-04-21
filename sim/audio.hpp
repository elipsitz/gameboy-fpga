#pragma once

#include <SDL2/SDL.h>

const int AUDIO_SAMPLE_RATE = 32768;
const int AUDIO_CHANNELS = 2;

class Audio {
public:
    Audio();
    ~Audio();

private:
	SDL_AudioDeviceID device;
};