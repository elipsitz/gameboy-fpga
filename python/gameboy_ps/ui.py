import abc
from enum import Enum
import importlib.resources
import logging
from typing import List
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from .controller import Button
from . import resources

def rgba_to_i16(r, g, b, a = 255):
    if a == 0:
        return COLOR_TRANSPARENT
    return 0x8000 | (r >> 3) << 0 | (g >> 3) << 5 | (b >> 3) << 10

def convert_image(image: Image) -> Image:
    output = Image.new("I", image.size)
    output_data = output.load()
    input_data = image.load()
    for i in range(image.size[0]):
        for j in range(image.size[1]):
            output_data[i, j] = rgba_to_i16(*input_data[i, j])
    return output

COLOR_TRANSPARENT = 0
COLOR_BG = rgba_to_i16(236, 236, 236)
COLOR_BLACK = rgba_to_i16(0, 0, 0)
COLOR_WHITE = rgba_to_i16(255, 255, 255)
COLOR_RED = rgba_to_i16(255, 0, 0)
COLOR_BLUE = rgba_to_i16(0, 255, 0)
COLOR_GREEN = rgba_to_i16(0, 0, 255)

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
            self.draw.font = self.font = ImageFont.truetype(r.open("rb"), 8)
        with (importlib.resources.files(resources) / "pixelmix_bold.ttf") as r:
            self.font_bold = ImageFont.truetype(r.open("rb"), 8)
        with (importlib.resources.files(resources) / "logo.png") as r:
            self.logo = convert_image(Image.open(r))

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
        self._select_widget = SelectWidget(["Run cartridge", "Load ROM file", "Options"])

    def on_attach(self) -> None:
        self._render()

    def on_button_event(self, button: Button, event: ButtonEvent) -> None:
        if event == ButtonEvent.PRESSED:
            if button == Button.UP:
                self._select_widget.move_up()

            if button == Button.DOWN:
                self._select_widget.move_down()

            if button == Button.A:
                if self._select_widget.pos == 0:
                    # Run cartridge
                    self.ui.set_screen(GameScreen(self.ui, None))
                    return
                if self._select_widget.pos == 1:
                    # Load ROM file
                    self.ui.set_screen(RomSelectScreen(self.ui))
                    return

        self._render()

    def _render(self) -> None:
        self.ui.draw.rectangle([(0, 0), (self.ui.width, self.ui.height)], fill=COLOR_BG)
        self.ui.framebuffer.paste(self.ui.logo, (15, 24))
        self._select_widget.render(self.ui, 30, 70, 100, 50)
        self.ui.show_framebuffer()


class GameScreen(Screen):
    def __init__(self, ui: UI, rom_path: Path) -> None:
        self.ui = ui
        self.playing = True
        self._widget = SelectWidget(["Resume", "Reset", "Main Menu"])

        if rom_path is None:
            self.ui.system.gameboy.set_physical_cartridge()
        else:
            self.ui.system.gameboy.set_emulated_cartridge(rom_path)
        self.ui.system.gameboy.reset()

    def on_attach(self) -> None:
        self.ui.system.gameboy.set_paused(False)

    def on_button_event(self, button: Button, event: ButtonEvent) -> None:
        if self.playing:
            if button == Button.HOME and event == ButtonEvent.PRESSED:
                self.playing = False
                self.ui.system.gameboy.set_paused(True)
                self.ui.system.gameboy.persist_ram()
                self._widget.pos = 0
                self._render()
            return

        if event == ButtonEvent.PRESSED:
            if button == Button.UP:
                self._widget.move_up()
            if button == Button.DOWN:
                self._widget.move_down()
            if button == Button.HOME:
                self.ui.system.gameboy.set_paused(False)
                self.playing = True
                return
            if button == Button.A:
                if self._widget.pos == 0:
                    # Resume
                    self.ui.system.gameboy.set_paused(False)
                    self.playing = True
                    return
                if self._widget.pos == 1:
                    # Reset
                    self.ui.system.gameboy.reset()
                    self.ui.system.gameboy.set_paused(False)
                    self.playing = True
                    return
                if self._widget.pos == 2:
                    # Main Menu
                    self.ui.set_screen(MainMenuScreen(self.ui))
                    return
            self._render()

    def _render(self) -> None:
        self.ui.draw.rectangle([(0, 0), (self.ui.width, self.ui.height)], fill=COLOR_TRANSPARENT)
        self.ui.draw.rectangle([(30, 40), (130, 110)], fill=COLOR_BG)
        self._widget.render(self.ui, 40, 50, 80, 50)
        self.ui.show_framebuffer()


class RomSelectScreen(Screen):
    def __init__(self, ui: UI) -> None:
        self.ui = ui
        rom_directory = self.ui.system.rom_directory
        self.roms = []
        self.roms.extend(rom_directory.glob("*.gb"))
        self.roms.extend(rom_directory.glob("*.gbc"))
        self.roms.sort()
        rom_filenames = [str(x.relative_to(rom_directory)) for x in self.roms]
        self._widget = ListWidget(rom_filenames, lines=9)

    def on_attach(self) -> None:
        self._render()

    def on_button_event(self, button: Button, event: ButtonEvent) -> None:
        if event == ButtonEvent.PRESSED:
            if button == Button.UP:
                self._widget.move_up()

            if button == Button.DOWN:
                self._widget.move_down()

            if button == Button.B:
                self.ui.set_screen(MainMenuScreen(self.ui))
                return

            if button == Button.A:
                rom_path = self.roms[self._widget.pos]
                self.ui.set_screen(GameScreen(self.ui, rom_path))
                return

        self._render()
        
    def _render(self) -> None:
        self.ui.draw.rectangle([(0, 0), (self.ui.width, self.ui.height)], fill=COLOR_BG)
        self.ui.draw.rectangle([(4, 16), (160 - 8, 144 - 16)], outline=COLOR_BLACK)
        self.ui.draw.text(
            (4, 4),
            "Load ROM file...",
            fill=COLOR_BLACK,
            font=self.ui.font_bold,
        )
        self.ui.draw.text(
            (4, 144 - 12),
            "A: Select              B: Back",
            fill=COLOR_BLACK,
        )
        self._widget.render(self.ui, 6, 18, 150, 108)
        self.ui.show_framebuffer()


class ListWidget:
    def __init__(self, items: List[str], lines: int) -> None:
        self.items = items
        self.pos = 0
        self.start = 0
        self.lines = lines
    
    def move_up(self) -> None:
        if self.pos > 0:
            self.pos -= 1
            if self.pos < self.start:
                self.start -= 1

    def move_down(self) -> None:
        if self.pos < len(self.items) - 1:
            self.pos += 1
            if self.pos >= self.start + self.lines:
                self.start += 1

    def render(self, ui: UI, x: int, y: int, w: int, h: int) -> None:
        # Draw cursor
        if len(self.items) > self.lines:
            cursor_unit = float(h) / len(self.items)
            cursor_y = int(cursor_unit * self.start)
            cursor_h = int(cursor_unit * self.lines)
            ui.draw.rectangle(
                [x + w - 2, y + cursor_y, x + w, y + cursor_y + cursor_h],
                fill=COLOR_BLACK)
        
        # Draw items
        y += 2
        for i, text in enumerate(self.items[self.start : (self.start + self.lines)]):
            # Ellipsize text if needed
            max_width = w - 8
            if ui.draw.font.getlength(text) > max_width:
                while ui.draw.font.getlength(text + "...") > max_width:
                    truncated = True
                    text = text[:-1]
                text = text + "..."
                
            bbox = ui.draw.textbbox((0, 0), text)
            text_w = bbox[2]
            text_h = bbox[3]
            ui.draw.text((x + 2, y), text, fill=COLOR_BLACK)
            if self.pos == (i + self.start):
                ui.draw.rectangle([x, y - 2, x + max_width + 2, y + text_h + 2], outline=COLOR_BLACK)
            y += 4 + text_h
            i += 1


class SelectWidget:
    def __init__(self, items: List[str]) -> None:
        self.items = items
        self.pos = 0

    def move_up(self) -> None:
        if self.pos > 0:
            self.pos -= 1

    def move_down(self) -> None:
        if self.pos < len(self.items) - 1:
            self.pos += 1
    
    def render(self, ui: UI, x: int, y: int, w: int, h: int) -> None:
        y += 4
        for i, item in enumerate(self.items):
            bbox = ui.draw.textbbox((0, 0), item)
            text_w = bbox[2]
            text_h = bbox[3]
            ui.draw.text(
                (x + (w / 2) - (text_w / 2), y),
                item,
                fill=COLOR_BLACK
            )
            if self.pos == i:
                ui.draw.rectangle([x, y - 3, x + w, y + text_h + 3], outline=COLOR_BLACK)
            y += 8 + text_h

