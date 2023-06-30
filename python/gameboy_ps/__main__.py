#!/usr/bin/env python3

"""
Zynq PS side of the Gameboy, using the Pynq API to provide input and ROM loading.

Must be run as root.
"""

import signal
import time
import sys
import logging
logging.basicConfig(format='[%(asctime)s][%(levelname)s] %(message)s', level=logging.DEBUG)

from pathlib import Path

from .gameboy import Gameboy
from . import controller

# Set up Gameboy.
gameboy = Gameboy()

if len(sys.argv) < 2:
    logging.info("Using physical cartridge")
    gameboy.set_physical_cartridge()
else:
    rom_path = Path(sys.argv[1])
    logging.info("Loading ROM from %s", rom_path)
    gameboy.set_emulated_cartridge(rom_path)

# Set up controllers.
def controller_callback(button: controller.Button, pressed: bool) -> None:
    gameboy.set_button(button, pressed)
    
controllers = [c(controller_callback) for c in controller.CONTROLLER_LISTENERS]

# Start the game.
logging.info("Initialization complete.")
gameboy.set_paused(False)

# Wait.
try:
    signal.pause()
except KeyboardInterrupt:
    pass

gameboy.set_paused(True)
gameboy.persist_ram()
