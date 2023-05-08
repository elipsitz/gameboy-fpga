import os
import signal
import time

from xbox360controller import Xbox360Controller

print("Loading Pynq libraries...")
from pynq import allocate, GPIO, MMIO, Overlay
print("Finished loading Pynq libraries")

OVERLAY_FILENAME: str = "gameboy.bit"
OVERLAY_PATH: str = os.path.join(os.path.dirname(os.path.abspath(__file__)), OVERLAY_FILENAME)

JOYPAD_BUTTONS = ["start", "select", "b", "a", "down", "up", "left", "right"]

"""
Zynq PS side of the Gameboy, using the Pynq API to provide input and ROM loading.
"""

# Load the overlay
print("Loading overlay...")
start_time = time.time()
design = Overlay(OVERLAY_PATH)
duration = time.time() - start_time
print(f"Finished loading overlay in {duration} sec")

# Initialize PL/PS communication
gpio_joypad = {JOYPAD_BUTTONS[i]: GPIO(GPIO.get_gpio_pin(8 + i), "out") for i in range(len(JOYPAD_BUTTONS))}
for x in gpio_joypad.values():
    x.write(0)

# Set up controller listener
def on_hat_moved(axis):
    gpio_joypad["left"].write(int(axis.x < 0))
    gpio_joypad["right"].write(int(axis.x > 0))
    gpio_joypad["down"].write(int(axis.y < 0))
    gpio_joypad["up"].write(int(axis.y > 0))

print("Initializing controller")
controller = Xbox360Controller(0, axis_threshold=-1)
# A and B are swapped due to the different locations on the Xbox controller vs the Gameboy joypad
controller.button_a.when_pressed = lambda _: gpio_joypad["b"].write(1)
controller.button_a.when_released = lambda _: gpio_joypad["b"].write(0)
controller.button_b.when_pressed = lambda _: gpio_joypad["a"].write(1)
controller.button_b.when_released = lambda _: gpio_joypad["a"].write(0)
controller.button_select.when_pressed = lambda _: gpio_joypad["select"].write(1)
controller.button_select.when_released = lambda _: gpio_joypad["select"].write(0)
controller.button_start.when_pressed = lambda _: gpio_joypad["start"].write(1)
controller.button_start.when_released = lambda _: gpio_joypad["start"].write(0)
controller.hat.when_moved = on_hat_moved
print("Done initializing controller")

# Initialization complete.
print("Initialization complete.")

try:
    signal.pause()
except KeyboardInterrupt:
    pass
