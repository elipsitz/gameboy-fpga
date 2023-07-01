#!/usr/bin/env python3

import importlib.resources
import logging
import time
from pathlib import Path

logging.info("Loading Pynq libraries...")
from pynq import allocate, GPIO, MMIO, Overlay
import numpy as np
logging.info("Finished loading Pynq libraries")

from PIL import Image

from . import controller
from . import resources

WIDTH = 160
HEIGHT = 144

REGISTER_MMIO_ADDR = 0x43C0_0000
REGISTER_CONTROL = 0x0
REGISTER_EMU_CART_CONFIG = 0x4
REGISTER_ROM_ADDRESS = 0x8
REGISTER_ROM_MASK = 0xC
REGISTER_RAM_ADDRESS = 0x10
REGISTER_RAM_MASK = 0x14
REGISTER_DEBUG_CPU1 = 0x18
REGISTER_DEBUG_CPU2 = 0x1C
REGISTER_DEBUG_CPU3 = 0x20
REGISTER_STAT_STALLS = 0x24
REGISTER_STAT_CLOCKS = 0x28
REGISTER_BLIT_CONTROL = 0x2C
REGISTER_BLIT_ADDRESS = 0x30

JOYPAD_BUTTONS = [
    controller.Button.START, controller.Button.SELECT, controller.Button.B, controller.Button.A,
    controller.Button.DOWN, controller.Button.UP, controller.Button.LEFT, controller.Button.RIGHT,
]

class Gameboy:
    def __init__(self) -> None:
        self._paused = True
        self._reset = False
        self._emu_cartridge = False
        self._blit_active = False
        
        # Load the overlay
        logging.info("Loading overlay...")
        resource_dir = importlib.resources.files(resources)
        with importlib.resources.as_file(resource_dir / "gameboy.bit") as f:
            overlay_path = f.resolve()
        start_time = time.time()
        self.overlay = Overlay(str(overlay_path))
        duration = time.time() - start_time
        logging.info("Finished loading overlay in %f sec", duration)

        # Initialize PS/PL communication
        self._registers = MMIO(REGISTER_MMIO_ADDR, 64 * 1024)
        self._joypad = {JOYPAD_BUTTONS[i]: GPIO(GPIO.get_gpio_pin(8 + i), "out") for i in range(len(JOYPAD_BUTTONS))}
        for x in self._joypad.values():
            x.write(0)

        # Framebuffer for UI
        framebuffer_size = WIDTH * HEIGHT
        self._framebuffer = allocate(shape=(framebuffer_size, ), dtype="uint16")

    def _write_reg_control(self) -> None:
        value = 0
        value |= int(not self._paused) << 0
        value |= int(self._reset) << 1
        self._registers.write(REGISTER_CONTROL, value)

    def set_paused(self, paused: bool) -> None:
        """Set whether the Gameboy is paused"""
        if not paused:
            # Cannot unpause while blit is in progress.
            self._wait_for_blit_complete()
        self._paused = paused
        self._write_reg_control()

    def reset(self) -> None:
        """Reset the emulated Gameboy"""
        self._reset = True
        self._write_reg_control()
        time.sleep(0.01)
        self._reset = False
        self._write_reg_control()

    def set_physical_cartridge(self) -> None:
        """Configure the Gameboy to use the physical cartridge"""
        self._emu_cartridge = False
        self._registers.write(REGISTER_EMU_CART_CONFIG, 0)
        self._rom_buffer = None
        self._ram_buffer = None

    def _write_reg_emu_cart_config(self, mbc, has_ram = False, has_timer = False, has_rumble = False) -> None:
        value = 1  # Lowest bit: is emulated cartridge enabled
        value |= mbc << 1
        value |= int(has_ram) << 4
        value |= int(has_timer) << 5
        value |= int(has_rumble) << 6
        self._registers.write(REGISTER_EMU_CART_CONFIG, value)

    def set_emulated_cartridge(self, rom_path: Path) -> None:
        """Sets the use of an enumated cartridge"""
        self._emu_cartridge  = True
        rom_data = np.fromfile(open(rom_path, "rb"), dtype=np.uint8)
        rom_size = rom_data.shape[0]
        self._rom_buffer = allocate(shape=rom_data.shape, dtype="uint8")
        self._rom_buffer[:] = rom_data
        self._rom_buffer.sync_to_device()

        # Parse ROM header
        logging.info("Parsing ROM header...")
        EMU_CONFIGS = {
            0x00: dict(mbc=0),
            0x01: dict(mbc=1),
            0x02: dict(mbc=1, has_ram=True),
            0x03: dict(mbc=1, has_ram=True),
            0x0F: dict(mbc=3, has_timer=True),
            0x10: dict(mbc=3, has_ram=True, has_timer=True),
            0x11: dict(mbc=3),
            0x12: dict(mbc=3, has_ram=True),
            0x13: dict(mbc=3, has_ram=True),
            0x19: dict(mbc=4),
            0x1C: dict(mbc=4, has_rumble=True),
            0x1A: dict(mbc=4, has_ram=True),
            0x1B: dict(mbc=4, has_ram=True),
            0x1D: dict(mbc=4, has_ram=True, has_rumble=True),
            0x1E: dict(mbc=4, has_ram=True, has_rumble=True),
        }
        emu_config = EMU_CONFIGS[rom_data[0x147]]
        header_rom_size = 32 * 1024 * (1 << rom_data[0x148])
        header_ram_size = {0: 0, 2: (8 * 1024), 3: (32 * 1024), 4: (128 * 1024), 5: (64 * 1024)}[rom_data[0x149]]
        logging.info(f"Cart type: {rom_data[0x147]}, config={emu_config}")
        logging.info(f"ROM size: {header_rom_size}")
        logging.info(f"RAM size: {header_ram_size}")
        logging.info("Done parsing ROM information")

        # Allocate RAM
        self._ram_buffer = None
        if header_ram_size > 0:
            self._ram_buffer = allocate(shape=(header_ram_size, ), dtype="uint8")
            self._ram_buffer.fill(0xFF)

        # Load save file, if one exists.
        self._save_path = rom_path.with_suffix(".sav")
        if self._ram_buffer is not None and self._save_path.is_file():
            self._ram_buffer[:] = np.fromfile(self._save_path, dtype="uint8")
            logging.info("Loaded save file at %s", self._save_path)

        # Set registers
        self._write_reg_emu_cart_config(**emu_config)
        self._registers.write(REGISTER_ROM_ADDRESS, self._rom_buffer.device_address)
        self._registers.write(REGISTER_ROM_MASK, rom_size - 1)
        if self._ram_buffer is not None:
            self._registers.write(REGISTER_RAM_ADDRESS, self._ram_buffer.device_address)
            self._registers.write(REGISTER_RAM_MASK, header_ram_size - 1)
        else:
            self._registers.write(REGISTER_RAM_ADDRESS, 0)
            self._registers.write(REGISTER_RAM_MASK, 0)

    def persist_ram(self) -> None:
        """Persists battery-backed ram (if present) to disk."""
        if self._ram_buffer is None:
            return

        was_paused = self._paused
        self.set_paused(True)

        self._ram_buffer.sync_from_device()
        with open(self._save_path, "wb") as f:
            f.write(self._ram_buffer.tobytes())
        logging.info("Wrote save file to %s", self._save_path)

        if not was_paused:
            self.set_paused(False)
        
    def set_button(self, button: controller.Button, pressed: bool) -> None:
        """Sets the state of a button to pressed or unpressed."""
        if button in self._joypad:
            self._joypad[button].write(int(pressed))

    def _wait_for_blit_complete(self) -> None:
        if self._blit_active:
            while self._registers.read(REGISTER_BLIT_CONTROL) != 0:
                time.sleep(0.01)

        self._blit_active = False

    def copy_framebuffer(self, image: Image) -> None:
        """
        Copy an image to the PL framebuffer.
        
        Image should be 160x144, mode 'I', where the lower 16-bits are interpreted as:
        a bbbbb ggggg rrrrr

        a is transparency -- 1 for opaque, 0 for transparent
        """
        self._wait_for_blit_complete()
        self.set_paused(True)
        np.copyto(self._framebuffer, np.array(image).view(np.uint16).flatten()[0::2])
        self._registers.write(REGISTER_BLIT_ADDRESS, self._framebuffer.device_address)
        self._registers.write(REGISTER_BLIT_CONTROL, 1)
        self._blit_active = True
