#!/usr/bin/env python3

import importlib.resources
import logging
import time
from pathlib import Path
import struct
from typing import Dict

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
# Control
REGISTER_CONTROL = 0 * 4
# Emulated cartridge
REGISTER_EMU_CART_CONFIG = 32 * 4
REGISTER_ROM_ADDRESS = 33 * 4
REGISTER_ROM_MASK = 34 * 4
REGISTER_RAM_ADDRESS = 35 * 4
REGISTER_RAM_MASK = 36 * 4
REGISTER_RTC_STATE = 37 * 4
REGISTER_RTC_LATCHED = 38 * 4
# Framebuffer
REGISTER_BLIT_CONTROL = 64 * 4
REGISTER_BLIT_ADDRESS = 65 * 4
# Debug
REGISTER_DEBUG_CPU1 = 96 * 4
REGISTER_DEBUG_CPU2 = 97 * 4
REGISTER_DEBUG_CPU3 = 98 * 4
REGISTER_DEBUG_SERIAL = 99 * 4
# Stats
REGISTER_STAT_STALLS = 128 * 4
REGISTER_STAT_CLOCKS = 129 * 4
REGISTER_STAT_CACHE_HITS = 130 * 4
REGISTER_STAT_CACHE_MISSES = 131 * 4



JOYPAD_BUTTONS = [
    controller.Button.START, controller.Button.SELECT, controller.Button.B, controller.Button.A,
    controller.Button.DOWN, controller.Button.UP, controller.Button.LEFT, controller.Button.RIGHT,
]

class Gameboy:
    CLOCK_RATE = 8 * 1024 * 1024

    def __init__(self) -> None:
        self._paused = True
        self._reset = False
        self._emu_cartridge = False
        self._blit_active = False
        self._duration_playing = 0.0
        self._time_unpaused = None
        
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
        if self._paused != paused:
            # Changing pause state
            if paused:
                self._duration_playing += time.monotonic() - self._time_unpaused
            else:
                self._time_unpaused = time.monotonic()

        self._paused = paused
        self._write_reg_control()

    def reset(self) -> None:
        """Reset the emulated Gameboy"""
        self._reset = True
        self._write_reg_control()
        time.sleep(0.01)
        self._reset = False
        self._write_reg_control()
        self._time_unpaused = time.monotonic()
        self._duration_playing = 0.0

    def set_physical_cartridge(self) -> None:
        """Configure the Gameboy to use the physical cartridge"""
        self._emu_cartridge = False
        self._registers.write(REGISTER_EMU_CART_CONFIG, 0)
        self._rom_buffer = None
        self._ram_buffer = None

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
        self.rom_header = RomHeader(rom_data)
        logging.info(f"Cart type: {self.rom_header.cartridge_type}")
        logging.info(f"Ram? {self.rom_header.has_ram}  Rtc? {self.rom_header.has_rtc}  Rumble? {self.rom_header.has_rumble}")
        logging.info(f"ROM size: {self.rom_header.rom_size}")
        logging.info(f"RAM size: {self.rom_header.ram_size}")
        logging.info("Done parsing ROM information")

        # Allocate RAM
        self._ram_buffer = None
        if self.rom_header.ram_size > 0:
            self._ram_buffer = allocate(shape=(self.rom_header.ram_size, ), dtype="uint8")
            self._ram_buffer.fill(0xFF)

        # Load save file, if one exists.
        self._save_path = rom_path.with_suffix(".sav")
        if self._save_path.is_file():
            save_data = np.fromfile(self._save_path, dtype="uint8")
            if self._ram_buffer is not None:
                self._ram_buffer[:] = save_data[:self.rom_header.ram_size]
                logging.info("Loaded save file at %s", self._save_path)
            if self.rom_header.has_rtc and (len(save_data) - self.rom_header.ram_size == 48):
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
        self._registers.write(REGISTER_EMU_CART_CONFIG, self.rom_header.get_emu_cart_config())
        self._registers.write(REGISTER_ROM_ADDRESS, self._rom_buffer.device_address)
        self._registers.write(REGISTER_ROM_MASK, rom_size - 1)
        if self._ram_buffer is not None:
            self._registers.write(REGISTER_RAM_ADDRESS, self._ram_buffer.device_address)
            self._registers.write(REGISTER_RAM_MASK, self.rom_header.ram_size - 1)
        else:
            self._registers.write(REGISTER_RAM_ADDRESS, 0)
            self._registers.write(REGISTER_RAM_MASK, 0)

    def persist_ram(self) -> None:
        """Persists battery-backed ram (if present) to disk."""
        if not self._emu_cartridge:
            return
        has_rtc = self.rom_header.has_rtc
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

    def get_stats(self) -> Dict[str, int]:
        return {
            "stalls": self._registers.read(REGISTER_STAT_STALLS),
            "clocks": self._registers.read(REGISTER_STAT_CLOCKS),
            "cache_hits": self._registers.read(REGISTER_STAT_CACHE_HITS),
            "cache_misses": self._registers.read(REGISTER_STAT_CACHE_MISSES),
        }
    
    def get_playtime(self) -> float:
        """Get the time (in seconds) the Game Boy has been playing since the last reset."""
        if self._paused:
            return self._duration_playing
        else:
            return self._duration_playing + (time.monotonic() - self._time_unpaused)


class RomHeader:
    def __init__(self, rom_data: bytes) -> None:
        self.mbc = 0
        self.has_ram = False
        self.has_rtc = False
        self.has_rumble = False

        emu_configs = {
            0x00: dict(mbc=0),
            0x01: dict(mbc=1),
            0x02: dict(mbc=1, has_ram=True),
            0x03: dict(mbc=1, has_ram=True),
            0x05: dict(mbc=2, has_ram=True),
            0x06: dict(mbc=2, has_ram=True),
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
        self.cartridge_type = rom_data[0x147]
        if self.cartridge_type not in emu_configs:
            raise RomLoadException(f"Unsupported cart {hex(self.cartridge_type)}")
        self.__dict__.update(emu_configs[self.cartridge_type])

        self.rom_size = 32 * 1024 * (1 << rom_data[0x148])
        self.ram_size = {0: 0, 2: (8 * 1024), 3: (32 * 1024), 4: (128 * 1024), 5: (64 * 1024)}[rom_data[0x149]]
        if self.mbc == 2:
            self.ram_size = 512

    def get_emu_cart_config(self) -> int:
        value = 1  # Lowest bit: is emulated cartridge enabled
        value |= self.mbc << 1
        value |= int(self.has_ram) << 4
        value |= int(self.has_rtc) << 5
        value |= int(self.has_rumble) << 6
        return value


class RomLoadException(Exception):
    pass


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
        def compute_ticks(value, ticks, wrap_point, max_value):
            if value >= wrap_point:
                needed = max_value - value
                if ticks >= needed:
                    ticks -= needed
                    value = 0
                else:
                    value += ticks
                    ticks = 0

            value += ticks
            next_ticks = value // wrap_point
            value = value % wrap_point
            return value, next_ticks

        ticks = seconds
        self.seconds, ticks = compute_ticks(self.seconds, ticks, 60, 2 ** 6)
        self.minutes, ticks = compute_ticks(self.minutes, ticks, 60, 2 ** 6)
        self.hours, ticks = compute_ticks(self.hours, ticks, 24, 2 ** 5)
        self.days, ticks = compute_ticks(self.days, ticks, 2 ** 9, 2 ** 9)
        if ticks > 0:
            self.days_overflow = True
