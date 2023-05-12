#pragma once

#include <memory>
#include <vector>

class Cartridge {
public:
    Cartridge(std::vector<uint8_t> rom);

    std::vector<uint8_t> rom;
    std::vector<uint8_t> ram;
    int mbc_type;
    bool has_ram;
    bool has_rumble;
    bool has_timer;
};