#!/usr/bin/env python3

import importlib.resources
import logging
import time
from pathlib import Path
import struct

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
REGISTER_RTC_STATE = 0x34
REGISTER_RTC_LATCHED = 0x38

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

    def _write_reg_emu_cart_config(self, mbc, has_ram = False, has_rtc = False, has_rumble = False) -> None:
        value = 1  # Lowest bit: is emulated cartridge enabled
        value |= mbc << 1
        value |= int(has_ram) << 4
        value |= int(has_rtc) << 5
        value |= int(has_rumble) << 6
        self._registers.write(REGISTER_EMU_CART_CONFIG, value)

    def set_emulated_cartridge(self, rom_path: Path) -> None:
        """Sets the use of an enumated cartridge"""
        self._emu_cartridge = True
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
            0x0F: dict(mbc=3, has_rtc=True),
            0x10: dict(mbc=3, has_ram=True, has_rtc=True),
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
        self._emu_cart_config = EMU_CONFIGS[rom_data[0x147]]
        header_rom_size = 32 * 1024 * (1 << rom_data[0x148])
        header_ram_size = {0: 0, 2: (8 * 1024), 3: (32 * 1024), 4: (128 * 1024), 5: (64 * 1024)}[rom_data[0x149]]
        logging.info(f"Cart type: {rom_data[0x147]}, config={self._emu_cart_config}")
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
        if self._save_path.is_file():
            save_data = np.fromfile(self._save_path, dtype="uint8")
            if self._ram_buffer is not None:
                self._ram_buffer[:] = save_data[:header_ram_size]
                logging.info("Loaded save file at %s", self._save_path)
            if self._emu_cart_config["has_rtc"] and (len(save_data) - header_ram_size == 48):
                # Load saved RTC data
                rtc_data = bytes(save_data[-48:])
                rtc_state = RtcState.from_disk(rtc_data[0:20])
                rtc_latched = RtcState.from_disk(rtc_data[20:40])
                (rtc_timestamp, ) = struct.unpack("<Q", rtc_data[40:48])
                elapsed = max(0, int(time.time()) - rtc_timestamp)
                rtc_state.advance(elapsed)
                self._registers.write(REGISTER_RTC_STATE, rtc_state.to_fpga())
                self._registers.write(REGISTER_RTC_LATCHED, rtc_latched.to_fpga())
                logging.info("Loaded saved RTC data")

        # Set registers
        self._write_reg_emu_cart_config(**self._emu_cart_config)
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
        if not self._emu_cartridge:
            return
        has_rtc = self._emu_cart_config["has_rtc"]
        if self._ram_buffer is None and not has_rtc:
            return

        was_paused = self._paused
        self.set_paused(True)

        with open(self._save_path, "wb") as f:
            if self._ram_buffer is not None:
                self._ram_buffer.sync_from_device()
                f.write(self._ram_buffer.tobytes())
                logging.info("Wrote save file to %s", self._save_path)
            if has_rtc:
                rtc_state = self._registers.read(REGISTER_RTC_STATE)
                rtc_latched = self._registers.read(REGISTER_RTC_LATCHED)
                f.write(RtcState.from_fpga(rtc_state).to_disk())
                f.write(RtcState.from_fpga(rtc_latched).to_disk())
                f.write(struct.pack("<Q", int(time.time())))
                logging.info("Wrote RTC data")

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

class RtcState:
    seconds: int
    minutes: int
    hours: int
    days: int
    days_overflow: bool
    halt: bool

    @staticmethod
    def from_fpga(data: int) -> "RtcState":
        # FPGA format: hodddddddddhhhhhmmmmmmssssss
        state = RtcState()
        state.seconds = (data >> 0) & 0b111111
        state.minutes = (data >> 6) & 0b111111
        state.hours = (data >> 12) & 0b11111
        state.days = (data >> 17) & 0b111111111
        state.days_overflow = bool((data >> 26) & 0b1)
        state.halt = bool((data >> 27) & 0b1)
        return state
    
    def to_fpga(self) -> int:
        data = 0
        data |= (self.seconds & 0b111111) << 0
        data |= (self.minutes & 0b111111) << 6
        data |= (self.hours & 0b11111) << 12
        data |= (self.days & 0b111111111) << 17
        data |= (int(self.days_overflow) & 0b1) << 26
        data |= (int(self.halt) & 0b1) << 27
        return data
    
    @staticmethod
    def from_disk(data: bytes) -> "RtcState":
        state = RtcState()
        unpacked = struct.unpack("<IIIII", data)
        state.seconds = unpacked[0] & 0b111111
        state.minutes = unpacked[1] & 0b111111
        state.hours = unpacked[2] & 0b11111
        state.days = (unpacked[3] & 0xFF) | ((unpacked[4] & 1) << 8)
        state.halt = (unpacked[4] >> 6) & 1
        state.days_overflow = (unpacked[4] >> 7) & 1
        return state
    
    def to_disk(self) -> bytes:
        return struct.pack(
            "<IIIII",
            self.seconds,
            self.minutes,
            self.hours,
            self.days & 0xFF,
            ((self.days & 0x100) >> 8) | (int(self.halt) << 6) | (int(self.days_overflow) << 7),
        )
    
    def advance(self, seconds: int) -> None:
        # TODO
        pass