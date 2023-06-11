#!/usr/bin/env python3

"""
Zynq PS side of the Gameboy, using the Pynq API to provide input and ROM loading.

Must be run as root.
"""

import os
import signal
import time
import sys
import logging
from pathlib import Path

# Local imports
import controller

logging.basicConfig(format='[%(asctime)s][%(levelname)s] %(message)s', level=logging.DEBUG)

logging.info("Loading Pynq libraries...")
from pynq import allocate, GPIO, MMIO, Overlay
import numpy as np
logging.info("Finished loading Pynq libraries")

OVERLAY_FILENAME: str = "gameboy.bit"
OVERLAY_PATH: str = os.path.join(os.path.dirname(os.path.abspath(__file__)), OVERLAY_FILENAME)

JOYPAD_BUTTONS = [
    controller.Button.START, controller.Button.SELECT, controller.Button.B, controller.Button.A,
    controller.Button.DOWN, controller.Button.UP, controller.Button.LEFT, controller.Button.RIGHT,
]

REGISTER_MMIO_ADDR = 0x43C0_0000
REGISTER_CONTROL = 0x0
REGISTER_EMU_CART_CONFIG = 0x4
REGISTER_ROM_ADDRESS = 0x8
REGISTER_ROM_MASK = 0xC
REGISTER_RAM_ADDRESS = 0x10
REGISTER_RAM_MASK = 0x14

def emu_cart_config(mbc, has_ram = False, has_timer = False, has_rumble = False):
    return 1 | (mbc << 1) | (int(has_ram) << 4) | (int(has_timer) << 5) | (int(has_rumble) << 6)

# Load the overlay
logging.info("Loading overlay...")
start_time = time.time()
design = Overlay(OVERLAY_PATH)
duration = time.time() - start_time
logging.info("Finished loading overlay in %f sec", duration)

# Load the game ROM
logging.info("Loading ROM...")
rom_path = Path(sys.argv[1])
rom_data = np.fromfile(open(rom_path, "rb"), dtype=np.uint8)
rom_size = rom_data.shape[0]
rom_buffer = allocate(shape=rom_data.shape, dtype="uint8")
rom_buffer[:] = rom_data
rom_buffer.sync_to_device()
logging.info("Done loading ROM.")

# Parse ROM header
logging.info("Parsing ROM information...")
EMU_CONFIGS = {
    0x00: emu_cart_config(0),
    0x01: emu_cart_config(1),
    0x02: emu_cart_config(1, has_ram=True),
    0x03: emu_cart_config(1, has_ram=True),
    0x0F: emu_cart_config(3, has_timer=True),
    0x10: emu_cart_config(3, has_ram=True, has_timer=True),
    0x11: emu_cart_config(3),
    0x12: emu_cart_config(3, has_ram=True),
    0x13: emu_cart_config(3, has_ram=True),
    0x19: emu_cart_config(4),
    0x1C: emu_cart_config(4, has_rumble=True),
    0x1A: emu_cart_config(4, has_ram=True),
    0x1B: emu_cart_config(4, has_ram=True),
    0x1D: emu_cart_config(4, has_ram=True, has_rumble=True),
    0x1E: emu_cart_config(4, has_ram=True, has_rumble=True),

}
emu_config = EMU_CONFIGS[rom_data[0x147]]
header_rom_size = 32 * 1024 * (1 << rom_data[0x148])
header_ram_size = {0: 0, 2: (8 * 1024), 3: (32 * 1024), 4: (128 * 1024), 5: (64 * 1024)}[rom_data[0x149]]
logging.info(f"Cart type: {rom_data[0x147]}, config={bin(emu_config)}")
logging.info(f"ROM size: {header_rom_size}")
logging.info(f"RAM size: {header_ram_size}")
logging.info("Done parsing ROM information")

# Allocate RAM
ram_buffer = None
if header_ram_size > 0:
    ram_buffer = allocate(shape=(header_ram_size, ), dtype="uint8")
    ram_buffer.fill(0xFF)

# Load save file, if one exists.
save_path = rom_path.with_suffix(".sav")
if ram_buffer is not None and save_path.is_file():
    ram_buffer[:] = np.fromfile(save_path, dtype="uint8")
    logging.info(f"Loaded save file at {save_path}")

# Initialize PL/PS communication
gpio_joypad = {JOYPAD_BUTTONS[i]: GPIO(GPIO.get_gpio_pin(8 + i), "out") for i in range(len(JOYPAD_BUTTONS))}
for x in gpio_joypad.values():
    x.write(0)
registers = MMIO(REGISTER_MMIO_ADDR, 64 * 1024)

# Set up controller listeners
def controller_callback(button, pressed):
    if button in gpio_joypad:
        gpio_joypad[button].write(int(pressed))
        
controllers = [c(controller_callback) for c in controller.CONTROLLER_LISTENERS]

# Initialization complete.
logging.info("Initialization complete.")

# Start the game.
registers.write(REGISTER_EMU_CART_CONFIG, emu_config)
registers.write(REGISTER_ROM_ADDRESS, rom_buffer.device_address)
registers.write(REGISTER_ROM_MASK, rom_size - 1)
if ram_buffer is not None:
    registers.write(REGISTER_RAM_ADDRESS, ram_buffer.device_address)
    registers.write(REGISTER_RAM_MASK, header_ram_size - 1)
else:
    registers.write(REGISTER_RAM_ADDRESS, 0)
    registers.write(REGISTER_RAM_MASK, 0)
registers.write(REGISTER_CONTROL, 0x1)

# Wait.
try:
    signal.pause()
except KeyboardInterrupt:
    pass

# Persist RAM to file
# TODO: only do this if it's battery-backed?
registers.write(REGISTER_CONTROL, 0x0)  # Pause game.
rom_buffer.sync_from_device()
if ram_buffer is not None:
    with open(save_path, "wb") as f:
        f.write(ram_buffer.tobytes())
    print(f"Wrote save file to {save_path}")
