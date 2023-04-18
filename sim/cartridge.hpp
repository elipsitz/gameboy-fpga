#pragma once

#include <memory>
#include <vector>

class Cartridge {
public:
    static std::unique_ptr<Cartridge> create(std::vector<uint8_t> rom);

    Cartridge(std::vector<uint8_t> rom, size_t ram_size) : rom(rom) {
        ram.resize(ram_size, 0xFF);
    }

    virtual ~Cartridge() = default;
    virtual uint8_t read(uint16_t address, bool select_rom);
    virtual void write(uint16_t address, bool select_rom, uint8_t data);

protected:
    std::vector<uint8_t> rom;
    std::vector<uint8_t> ram;
    size_t rom_bank_a = 0;
    size_t rom_bank_b = 1;
    size_t ram_bank = 0;
    bool ram_enable = false;
};