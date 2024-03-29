#!/usr/bin/env python3

import signal
import time
import logging

from pathlib import Path
from typing import Optional

from .gameboy import Gameboy
from . import controller, ui

class System:
    def __init__(self, rom_directory: Path):
        self.gameboy = Gameboy()
        self.buttons = {e: False for e in controller.Button}
        self.ui = ui.UI(self)
        self.rom_directory = rom_directory

        # Set up controllers.
        def controller_callback(button: controller.Button, pressed: bool) -> None:
            self.gameboy.set_button(button, pressed)

            was_pressed = self.buttons[button]
            if was_pressed != pressed:
                self.buttons[button] = pressed
                self.ui.on_button_state(button, pressed)     
    
        controllers = [c(controller_callback) for c in controller.CONTROLLER_LISTENERS]

        logging.info("Initialization complete.")

    def start(self) -> None:
        # self.gameboy.set_paused(False)

        # Wait.
        try:
            signal.pause()
        except KeyboardInterrupt:
            pass

        self.gameboy.set_paused(True)
        self.gameboy.persist_ram()
