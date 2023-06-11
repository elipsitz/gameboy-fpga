from abc import ABC
from enum import Enum
import logging
from typing import Callable

from xbox360controller import Xbox360Controller

class Button(Enum):
    START = 0
    SELECT = 1
    B = 2
    A = 3
    DOWN = 4
    UP = 5
    LEFT = 6
    RIGHT = 7
    HOME = 8

ControllerCallback = Callable[[Button, bool], None]

class Controller:
    pass

class XboxController(Controller):
    def __init__(self, callback: ControllerCallback):
        def on_hat_moved(axis):
            callback(Button.LEFT, axis.x < 0)
            callback(Button.RIGHT, axis.x > 0)
            callback(Button.DOWN, axis.y < 0)
            callback(Button.UP, axis.y > 0)

        try:
            controller = Xbox360Controller(0, axis_threshold=-1)
        except:
            logging.warning("No Xbox controllers found")
            return

        # A and B are swapped due to the different locations on the Xbox controller vs the Gameboy joypad
        controller.button_a.when_pressed = lambda _: callback(Button.B, True)
        controller.button_a.when_released = lambda _: callback(Button.B, False)
        controller.button_b.when_pressed = lambda _: callback(Button.A, True)
        controller.button_b.when_released = lambda _: callback(Button.A, False)
        controller.button_select.when_pressed = lambda _: callback(Button.SELECT, True)
        controller.button_select.when_released = lambda _: callback(Button.SELECT, False)
        controller.button_start.when_pressed = lambda _: callback(Button.START, True)
        controller.button_start.when_released = lambda _: callback(Button.START, False)
        controller.hat.when_moved = on_hat_moved
        logging.info("Initialized Xbox controller")


CONTROLLER_LISTENERS = [
    XboxController,
]
