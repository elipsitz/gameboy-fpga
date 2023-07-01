#!/usr/bin/env python3

import signal
import time
import logging

from pathlib import Path
from typing import Optional

from .gameboy import Gameboy
from . import controller

class System:
    def __init__(self, rom_path: Optional[Path]):
        self.gameboy = Gameboy()
        self.buttons = {e.value: False for e in controller.Button}

        if rom_path is None:
            logging.info("Using physical cartridge")
            self.gameboy.set_physical_cartridge()
        else:
            logging.info("Loading ROM from %s", rom_path)
            self.gameboy.set_emulated_cartridge(rom_path)

        # Set up controllers.
        def controller_callback(button: controller.Button, pressed: bool) -> None:
            was_pressed = self.buttons[button]
            if was_pressed != pressed:
                self.buttons[button] = pressed

            self.gameboy.set_button(button, pressed)
    
        controllers = [c(controller_callback) for c in controller.CONTROLLER_LISTENERS]

        logging.info("Initialization complete.")

    def start(self) -> None:
        self.gameboy.set_paused(False)

        # Wait.
        try:
            signal.pause()
        except KeyboardInterrupt:
            pass

        self.gameboy.set_paused(True)
        self.gameboy.persist_ram()
