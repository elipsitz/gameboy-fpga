#include "audio.hpp"
#include "common.hpp"
#include "simulator.hpp"

// "hollow knight inspired" palette
//static const uint32_t palette[4] = {0xfafbf6, 0xc6b7be, 0x565a75, 0x0f0f1b};
// gray palette
//static const uint32_t palette[4] = {0xffffff, 0xaaaaaa, 0x555555, 0x000000};

Simulator::Simulator(std::unique_ptr<Cartridge> cart) : cart(std::move(cart))
{
    this->top = new VSimGameboy;
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
    top->io_clockConfig_enable = true;
    top->io_dataAccess_dataRead = 0;
    top->io_cartConfig_mbcType = cart->mbc_type;
    top->io_cartConfig_hasRam = cart->has_ram;
    top->io_cartConfig_hasRtc = cart->has_timer;
    top->io_cartConfig_hasRumble = cart->has_rumble;
    top->reset = 1;

    uint64_t total = 8 - (cycles % 8);
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
    bool prevAccessEnable = false;

    for (uint64_t i = 0; i < num_cycles * 2; i++) {
        top->io_clockConfig_provide8Mhz = top->io_clockConfig_need8Mhz;
        if (!top->io_clockConfig_provide8Mhz) {
            i++;
        }

        // Handle memory.
        if (top->io_dataAccess_enable && !prevAccessEnable) {
            std::vector<uint8_t>& mem = top->io_dataAccess_selectRom ? cart->rom : cart->ram;

            if (top->io_dataAccess_write) {
                mem[top->io_dataAccess_address % mem.size()] = top->io_dataAccess_dataWrite;
            } else {
                top->io_dataAccess_dataRead = mem[top->io_dataAccess_address % mem.size()];
            }
            top->io_dataAccess_valid = true;
        }
        prevAccessEnable = top->io_dataAccess_enable;

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

        uint16_t pixel = top->io_ppu_pixel;
        uint8_t r = (pixel >> 0) & 0x1F;
        uint8_t g = (pixel >> 5) & 0x1F;
        uint8_t b = (pixel >> 10) & 0x1F;
        framebuffer[framebufferIndex++] = (r << 3) | (r >> 2);
        framebuffer[framebufferIndex++] = (g << 3) | (g >> 2);
        framebuffer[framebufferIndex++] = (b << 3) | (b >> 2);
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
        audioSampleBuffer.push_back(left * 8);
        audioSampleBuffer.push_back(right * 8);

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