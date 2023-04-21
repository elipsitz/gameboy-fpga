#include <algorithm>
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
    // Unpause audio device.
    SDL_PauseAudioDevice(device, 0);
}

// Each sample is a sample 16-bit (left, right) pair (4 bytes total per sample).
void Audio::push(uint16_t* data, uint32_t length) {
    uint32_t samples_queued = SDL_GetQueuedAudioSize(device) / 4;
    // Target maximum of 2 frames of samples in the buffer.
    uint32_t samples_max = 2 * AUDIO_SAMPLE_RATE / 60;

    if (samples_queued < samples_max) {
        uint32_t samples = std::min(length / 2, samples_max - samples_queued);
        SDL_QueueAudio(device, data, samples * 4);
    }
}

Audio::~Audio() {
    SDL_CloseAudioDevice(device);
}