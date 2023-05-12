#include "cartridge.hpp"

#include <stdio.h>

Cartridge::Cartridge(std::vector<uint8_t> rom) : rom(rom) {
    uint8_t metadataType = rom[0x147];
    uint8_t metadataRomSize = rom[0x148];
    uint8_t metadataRamSize = rom[0x149];

    size_t rom_size = 32 * 1024 * (1 << (size_t)metadataRomSize);
    size_t ram_size = 0;
    switch (metadataRamSize) {
        case 2: ram_size = 8 * 1024; break;
        case 3: ram_size = 32 * 1024; break;
        case 4: ram_size = 128 * 1024; break;
        case 5: ram_size = 64 * 1024; break;
    }

    if (rom_size != rom.size()) {
        throw std::runtime_error("Rom size mismatch");
    }

    has_ram = false;
    has_rumble = false;
    has_timer = false;

    switch (metadataType) {
        // ROM ONLY
        case 0x00:
            mbc_type = 0;
            break;
        // MBC1
        case 0x01:
            mbc_type = 1;
            break;
        // MBC1+RAM, MBC2+RAM+BATTERY
        case 0x02:
        case 0x03:
            mbc_type = 1;
            has_ram = true;
            break;
        // MBC3+TIMER+BATTERY
        case 0x0F:
            mbc_type = 3;
            has_timer = true;
            break;
        // MBC3+TIMER+RAM+BATTERY
        case 0x10:
            mbc_type = 3;
            has_timer = true;
            has_ram = true;
            break;
        // MBC3
        case 0x11:
            mbc_type = 3;
            break;
        // MBC3+RAM
        case 0x12:
            mbc_type = 3;
            has_ram = true;
            break;
        // MBC3+RAM+BATTERY
        case 0x13:
            mbc_type = 3;
            has_ram = true;
        case 0x19: // MBC5
            mbc_type = 4;
            break;
        case 0x1C: // MBC5+RUMBLE
            mbc_type = 4;
            has_rumble = true;
            break;
        case 0x1A: // MBC5+RAM
        case 0x1B: // MBC5+RAM+BATTERY
            mbc_type = 4;
            has_ram = true;
            break;
        case 0x1D: // MBC5+RUMBLE+RAM
        case 0x1E: // MBC5+RUMBLE+RAM+BATTERY
            mbc_type = 4;
            has_ram = true;
            has_rumble = true;
            break;
        default:
            throw std::runtime_error("Unknown cartridge type");
    }

    ram.resize(ram_size, 0xFF);
}
