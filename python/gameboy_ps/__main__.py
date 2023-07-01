#!/usr/bin/env python3

"""
Zynq PS side of the Gameboy, using the Pynq API to provide input and ROM loading.

Must be run as root.
"""

import logging
logging.basicConfig(format='[%(asctime)s][%(levelname)s] %(message)s', level=logging.DEBUG)

from . import system
system = system.System()
system.start()
