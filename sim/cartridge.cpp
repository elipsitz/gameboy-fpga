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
        if (ram_enable && ram.size() > 0) {
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

class CartridgeMBC1 : public Cartridge {
public:
    CartridgeMBC1(std::vector<uint8_t> rom, size_t ram_size) : Cartridge(rom, ram_size) {}

    // Use superclass read

    void write(uint16_t address, bool select_rom, uint8_t data) {
        if (!select_rom) {
            Cartridge::write(address, select_rom, data);
        } else if (address >= 0x0000 && address < 0x2000) {
            ram_enable = (data & 0xF) == 0xA;
        } else if (address >= 0x2000 && address < 0x4000) {
            bank_reg_0 = data & 0x1F;
            if (bank_reg_0 == 0) {
                bank_reg_0 = 1;
            }
        } else if (address >= 0x4000 && address < 0x6000) {
            bank_reg_1 = data & 0x3;
        } else if (address >= 0x6000 && address < 0x8000) {
            bank_mode = data;
        }

        if (bank_mode == 0) {
            rom_bank_a = 0;
            rom_bank_b = (bank_reg_0) | (bank_reg_1 << 5);
            ram_bank = 0;
        } else {
            rom_bank_a = bank_reg_1 << 5;
            rom_bank_b = (bank_reg_0) | (bank_reg_1 << 5);
            ram_bank = bank_reg_1;
        }
    }

private:
    uint8_t bank_reg_0 = 1;
    uint8_t bank_reg_1 = 0;
    uint8_t bank_mode = 0;
    bool ram_enable = false;
};


class CartridgeMBC3 : public Cartridge {
public:
    CartridgeMBC3(std::vector<uint8_t> rom, size_t ram_size, bool has_timer)
        : Cartridge(rom, ram_size) {
        this->has_timer = has_timer;
    }

    uint8_t read(uint16_t address, bool select_rom) {
        if (select_rom || (ram_bank & 0x8) == 0) {
            return Cartridge::read(address, select_rom);
        } else {
            // TODO clock registers
            return 0xFF;
        }
    }

    void write(uint16_t address, bool select_rom, uint8_t data) {
        if (address >= 0x0000 && address < 0x2000) {
            ram_enable = (data == 0x0A);
        } else if (address >= 0x2000 && address < 0x4000) {
            rom_bank_b = data & 0x7F;
            if (rom_bank_b == 0) {
                rom_bank_b = 1;
            }
        } else if (address >= 0x4000 && address < 0x6000) {
            ram_bank = data & 0xF;
        } else if (address >= 0x6000 && address < 0x8000) {
            // TODO clock: "latch clock data"
        }
    }

private:
    bool ram_enable = false;
    bool has_timer = false;
};

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
        // MBC1
        case 0x01:
            return std::make_unique<CartridgeMBC1>(rom, 0);
        // MBC1+RAM, MBC2+RAM+BATTERY
        case 0x02:
        case 0x03:
            return std::make_unique<CartridgeMBC1>(rom, ram_size);
        // MBC3+TIMER+BATTERY
        case 0x0F:
            return std::make_unique<CartridgeMBC3>(rom, 0, true);
        // MBC3+TIMER+RAM+BATTERY
        case 0x10:
            return std::make_unique<CartridgeMBC3>(rom, ram_size, true);
        // MBC3
        case 0x11:
            return std::make_unique<CartridgeMBC3>(rom, 0, false);
        // MBC3+RAM
        case 0x12:
            return std::make_unique<CartridgeMBC3>(rom, ram_size, false);
        // MBC3+RAM+BATTERY
        case 0x13:
            return std::make_unique<CartridgeMBC3>(rom, ram_size, false);
        default:
            throw std::runtime_error("Unknown cartridge type");
    }
}