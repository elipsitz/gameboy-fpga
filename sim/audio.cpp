#include <iostream>

#include "audio.hpp"
#include "common.hpp"
#include "window.hpp"

Audio::Audio() {
    SDL_AudioSpec want;
    SDL_memset(&want, 0, sizeof(want));
    want.freq = AUDIO_SAMPLE_RATE;
    want.format = AUDIO_S16SYS;
    want.channels = 2;
    // 256 is ~0.5 a frame of samples
    want.samples = 256;
    want.callback = nullptr;

    SDL_AudioSpec have;
    device = SDL_OpenAudioDevice(NULL, 0, &want, &have, 0);
    if (device == 0) {
        std::cout << "Failed to open audio device: " << SDL_GetError() << std::endl;
        return;
    }
}


Audio::~Audio() {
    SDL_CloseAudioDevice(device);
}