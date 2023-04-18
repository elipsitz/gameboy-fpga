#include "cartridge.hpp"

#include <stdio.h>

uint8_t Cartridge::read(uint16_t address, bool select_rom) {
    if (select_rom) {
        size_t index;
        if ((address & 0x4000) == 0) {
            index = (rom_bank_a << 14) | (address & 0x3FFF);
        } else {
            index = (rom_bank_b << 14) | (address & 0x3FFF);
        }
        return rom[index % rom.size()];
    } else {
        if (ram.size() > 0) {
            size_t index = (ram_bank << 13) | (address & 0x1FFF);
            return ram[index % ram.size()];
        } else {
            return 0xFF;
        }
    }
}

void Cartridge::write(uint16_t address, bool select_rom, uint8_t data) {
    if (!select_rom && ram.size() > 0) {
        size_t index = (ram_bank << 13) | (address & 0x1FFF);
        ram[index % ram.size()] = data;
    }
}

//class CartridgeRom : Cartridge {
//public:
//    CartridgeRom(std::vector<uint8_t> rom) : rom(rom) {}
//
//    uint8_t read(uint16_t address, bool select_rom) {
//        return Cartridge::read(address, select_rom);
//    }
//
//    void write(uint16_t address, bool select_rom, uint8_t data) {
//        Cartridge::write(address, select_rom, data);
//    }
//};

std::unique_ptr<Cartridge>
Cartridge::create(std::vector<uint8_t> rom)
{
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

    switch (metadataType) {
        // ROM ONLY
        case 0x00:
            return std::make_unique<Cartridge>(rom, ram_size);
        default:
            throw std::runtime_error("Unknown cartridge type");
    }
}