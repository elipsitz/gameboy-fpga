#include "audio.hpp"
#include "common.hpp"
#include "simulator.hpp"

// "hollow knight inspired" palette
//static const uint32_t palette[4] = {0xfafbf6, 0xc6b7be, 0x565a75, 0x0f0f1b};
// gray palette
static const uint32_t palette[4] = {0xffffff, 0xaaaaaa, 0x555555, 0x000000};

Simulator::Simulator(std::vector<uint8_t> rom)
{
    this->top = new VGameboy;
    this->cart = Cartridge::create(rom);
    this->framebuffer0.resize(WIDTH * HEIGHT * 4, 0xFF);
    this->framebuffer1.resize(WIDTH * HEIGHT * 4, 0xFF);

    reset();
}

Simulator::~Simulator()
{
    top->final();
    delete top;
}

void Simulator::reset()
{
    top->io_cartridge_dataRead = 0;
    top->reset = 1;

    uint64_t total = 4 - (cycles % 1);
    simulate_cycles(total);

    top->reset = 0;
}

void Simulator::set_joypad_state(JoypadState state)
{
    top->io_joypad_start = state.start;
    top->io_joypad_select = state.select;
    top->io_joypad_b = state.b;
    top->io_joypad_a = state.a;
    top->io_joypad_down = state.down;
    top->io_joypad_up = state.up;
    top->io_joypad_left = state.left;
    top->io_joypad_right = state.right;
}

void Simulator::simulate_cycles(uint64_t num_cycles)
{
    for (uint64_t i = 0; i < num_cycles; i++) {
        // Handle memory.
        if (this->cycles % 4 == 3) {
            uint16_t address = top->io_cartridge_address;
            bool rom_select = top->io_cartridge_chipSelect;
//             printf("mem access at [%.04X] => [%.02X]\n", address, top->io_cartridge_dataWrite);
            if (top->io_cartridge_readEnable) {
                uint8_t data = cart->read(address, rom_select);
//                 printf("cart read at [%.04X] => [%.02X]\n", address, data);
                top->io_cartridge_dataRead = data;
            } else if (top->io_cartridge_writeEnable) {
                uint8_t data = top->io_cartridge_dataWrite;
//                 printf("cart write at [%.04X] <= [%.02X]\n", address, data);
                cart->write(address, rom_select, data);
            }
        }

        this->stepFramebuffer();
        this->stepAudio();

        top->clock = 0;
        top->eval();
        top->clock = 1;
        top->eval();

        this->cycles++;
    }
}

void Simulator::stepFramebuffer()
{
    bool vblank = top->io_ppu_vblank && !prev_vblank;
    if (vblank) {
        framebufferIndex = 0;
        activeFramebuffer = !activeFramebuffer;
    }
    prev_hblank = top->io_ppu_hblank;
    prev_vblank = top->io_ppu_vblank;
    std::vector<uint8_t>& framebuffer = activeFramebuffer ? framebuffer1 : framebuffer0;

    if (top->io_ppu_valid) {
        if (framebufferIndex >= framebuffer.size() - 4) {
            // TODO: make this a fatal error (framebuffer overrun).
            return;
        }

        uint8_t pixel = top->io_ppu_pixel;
        uint32_t color = palette[pixel];
        framebuffer[framebufferIndex++] = (color >> 0) & 0xFF;
        framebuffer[framebufferIndex++] = (color >> 8) & 0xFF;
        framebuffer[framebufferIndex++] = (color >> 16) & 0xFF;
        framebuffer[framebufferIndex++] = 0xFF;
    }

    // Blank the screen if the LCD is disabled.
    if (prev_lcd_enabled && !top->io_ppu_lcdEnable) {
        std::fill(framebuffer.begin(), framebuffer.end(), 0xFF);
        framebufferIndex = framebuffer.size();
    }
    prev_lcd_enabled = top->io_ppu_lcdEnable;
}

void Simulator::simulate_frame()
{
    simulate_cycles(70224);
}

std::vector<uint8_t>& Simulator::getFramebuffer()
{
    // Return framebuffer we're not writing to.
    return activeFramebuffer ? framebuffer0 : framebuffer1;
}

void Simulator::stepAudio()
{

    audioTimer++;
    if (audioTimer == (CLOCK_RATE / AUDIO_SAMPLE_RATE)) {
        int16_t mask = 1U << (10 - 1);
        int16_t left = (top->io_apu_left ^ mask) - mask;
        int16_t right = (top->io_apu_right ^ mask) - mask;

        audioTimer = 0;
        audioSampleBuffer.push_back(left);
        audioSampleBuffer.push_back(right);

        // static int test_timer = 0;
        // test_timer++;
        // if (test_timer >= 8) {
        //     test_timer = 0;
        //     printf("sample: %d   , %d\n", top->io_apu_left, left);
        // }
    }
}

std::vector<int16_t>& Simulator::getAudioSampleBuffer()
{
    return audioSampleBuffer;
}