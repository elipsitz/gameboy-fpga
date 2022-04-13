#pragma once

#include <string>
#include <vector>

#include <SDL2/SDL.h>

class Window {
public:
    Window();
    ~Window();

    /// Update the window with the contents of the framebuffer.
    void update(std::vector<uint8_t>& framebuffer);

    void setTitle(const char* title);
    
    const int SCALE = 2;

private:
    SDL_Window* window = nullptr;
    SDL_Renderer* renderer = nullptr;
    SDL_Texture* texture = nullptr;
};
