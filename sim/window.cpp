#include <iostream>

#include "common.hpp"
#include "window.hpp"

Window::Window()
{
    // Create the window.
    this->window = SDL_CreateWindow(
        "Game Boy",
        SDL_WINDOWPOS_CENTERED,
        SDL_WINDOWPOS_CENTERED,
        WIDTH * SCALE,
        HEIGHT * SCALE,
        SDL_WINDOW_OPENGL | SDL_WINDOW_ALLOW_HIGHDPI
    );
    if (this->window == nullptr) {
        std::cout << "Failed to create window" << std::endl;
        return;
    }

    this->renderer = SDL_CreateRenderer(
        this->window,
        -1,
        SDL_RENDERER_ACCELERATED | SDL_RENDERER_PRESENTVSYNC
    );
    if (this->renderer == nullptr) {
        std::cout << "Failed to create renderer" << std::endl;
        return;
    }

    this->texture = SDL_CreateTexture(
        renderer,
        SDL_PIXELFORMAT_ARGB8888,
        SDL_TEXTUREACCESS_STREAMING,
        WIDTH,
        HEIGHT
    );
    if (this->texture == nullptr) {
        std::cout << "Failed to create texture" << std::endl;
        return;
    }
}

void Window::update(std::vector<uint8_t>& framebuffer)
{
    // Copy over the new data.
    uint8_t* source = framebuffer.data();
    void* pixels;
    int pitch;
    SDL_LockTexture(this->texture, NULL, &pixels, &pitch);
    uint8_t* dest = (uint8_t*)pixels;
    for (int y = 0; y < HEIGHT; y++) {
        memcpy(dest, source, 4 * WIDTH);
        source += 4 * WIDTH;
        dest += pitch;
    }
    SDL_UnlockTexture(this->texture);

    SDL_RenderClear(this->renderer);
    SDL_RenderCopy(this->renderer, this->texture, NULL, NULL);
    SDL_RenderPresent(this->renderer);
}

void Window::setTitle(const char* title)
{
    SDL_SetWindowTitle(this->window, title);
}

Window::~Window()
{
    if (this->texture != nullptr) {
        SDL_DestroyTexture(this->texture);
    }
    if (this->renderer != nullptr) {
        SDL_DestroyRenderer(this->renderer);
    }
    if (this->window != nullptr) {
        SDL_DestroyWindow(this->window);
    }
}