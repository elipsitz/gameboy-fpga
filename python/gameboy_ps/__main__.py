#!/usr/bin/env python3

"""
Zynq PS side of the Gameboy, using the Pynq API to provide input and ROM loading.

Must be run as root.
"""

import logging
logging.basicConfig(format='[%(asctime)s][%(levelname)s] %(message)s', level=logging.DEBUG)
import sys

from . import system

if len(sys.argv) < 2:
    rom_path = None
else:
    rom_path = Path(sys.argv[1])

system = system.System(rom_path)
system.start()
