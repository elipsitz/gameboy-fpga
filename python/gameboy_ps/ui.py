import abc
from enum import Enum
import importlib.resources
import logging

from PIL import Image, ImageDraw, ImageFont

from .controller import Button
from . import resources

def rgba_to_i16(r, g, b, a = 255):
    if a == 0:
        return COLOR_TRANSPARENT
    return 0x8000 | (r >> 3) << 0 | (g >> 3) << 5 | (b >> 3) << 10

COLOR_TRANSPARENT = 0
COLOR_BG = 0xF7BD
COLOR_BLACK = rgba_to_i16(0, 0, 0)
COLOR_WHITE = rgba_to_i16(255, 255, 255)
COLOR_RED = rgba_to_i16(255, 0, 0)

class ButtonEvent(Enum):
    PRESSED = 0
    RELEASED = 1

class UI:
    def __init__(self, system: "System") -> None:
        self.system = system
        self.width = 160
        self.height = 144
        self.framebuffer = Image.new("I", (self.width, self.height), COLOR_TRANSPARENT)
        self.draw = ImageDraw.Draw(self.framebuffer)
        with (importlib.resources.files(resources) / "pixelmix.ttf") as r:
            self.draw.font = ImageFont.truetype(r.open("rb"), 8)

        self.screen = MainMenuScreen(self)
        self.screen.on_attach()

    def on_button_state(self, button: Button, pressed: bool) -> None:
        if pressed:
            self.screen.on_button_event(button, ButtonEvent.PRESSED)
        else:
            self.screen.on_button_event(button, ButtonEvent.RELEASED)
        
    def set_screen(self, screen: "Screen") -> None:
        self.screen = screen
        self.screen.on_attach()

    def show_framebuffer(self) -> None:
        self.system.gameboy.copy_framebuffer(self.framebuffer)

class Screen(abc.ABC):
    def on_attach(self) -> None:
        ...

    def on_button_event(self, button: Button, event: ButtonEvent) -> None:
        ...

class MainMenuScreen(Screen):
    def __init__(self, ui: UI) -> None:
        self.ui = ui
        self._count = 0

    def on_attach(self) -> None:
        self._render()

    def on_button_event(self, button: Button, event: ButtonEvent) -> None:
        if event == ButtonEvent.PRESSED:
            if button == Button.UP:
                self._count += 1
            if button == Button.DOWN:
                self._count -= 1
        self._render()

    def _render(self) -> None:
        self.ui.draw.rectangle([(0, 0), (self.ui.width, self.ui.height)], fill=COLOR_BG)
        self.ui.draw.line([(10, 10), (150, 50)], COLOR_BLACK)
        self.ui.draw.text((10, 100), f"Hello world! {self._count}", font=None, fill=COLOR_BLACK)
        self.ui.show_framebuffer()