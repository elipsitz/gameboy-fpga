from abc import ABC
from enum import Enum
import logging
from typing import Callable
import threading
import time

from xbox360controller import Xbox360Controller
from smbus2 import SMBus

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


class WiiClassicController(Controller):
    """Wii classic controller over I2C"""
    I2C_ADDR = 0x52
    I2C_INIT_DELAY = 0.1
    I2C_READ_DELAY = 0.002
    I2C_BUS = 0
    I2C_CONNECT_DELAY = 0.1
    POLL_RATE = 100

    def __init__(self, callback: ControllerCallback):
        self._callback = callback
        self._bus = SMBus(self.I2C_BUS)

        t = threading.Thread(target=self._event_loop)
        t.start()

    def _event_loop(self):
        while True:
            # Attempt to initialize controller
            while True:
                try:
                    self._bus.read_byte(self.I2C_ADDR)

                    self._bus.write_byte_data(self.I2C_ADDR, 0xF0, 0x55)
                    time.sleep(self.I2C_INIT_DELAY)
                    self._bus.write_byte_data(self.I2C_ADDR, 0xFB, 0x00)
                    time.sleep(self.I2C_INIT_DELAY)

                    self._bus.write_byte(self.I2C_ADDR, 0xFE)
                    time.sleep(self.I2C_READ_DELAY)
                    controller_id = self._bus.read_byte(self.I2C_ADDR)
                    logging.info("Connected to Wii Classic Controller: ID=%d", controller_id)
                    break
                except OSError as error:
                    time.sleep(self.I2C_CONNECT_DELAY)

            # Poll controller
            while True:
                try:
                    self._bus.write_byte(self.I2C_ADDR, 0x0)
                    time.sleep(self.I2C_READ_DELAY)
                    data = [self._bus.read_byte(self.I2C_ADDR) for _ in range(0, 8)]

                    self._callback(Button.A, not bool(data[5] & (1 << 4)))
                    self._callback(Button.B, not bool(data[5] & (1 << 6)))
                    self._callback(Button.START, not bool(data[4] & (1 << 2)))
                    self._callback(Button.SELECT, not bool(data[4] & (1 << 4)))
                    self._callback(Button.HOME, not bool(data[4] & (1 << 3)))
                    self._callback(Button.LEFT, not bool(data[5] & (1 << 1)))
                    self._callback(Button.RIGHT, not bool(data[4] & (1 << 7)))
                    self._callback(Button.UP, not bool(data[5] & (1 << 0)))
                    self._callback(Button.DOWN, not bool(data[4] & (1 << 6)))

                    time.sleep(1.0 / self.POLL_RATE)
                except OSError:
                    logging.info("Disconnected from Wii Classic Controller")
                    break


CONTROLLER_LISTENERS = [
    XboxController,
    WiiClassicController,
]
