import abc
from enum import Enum
import importlib.resources
import logging
from typing import List
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from .controller import Button
from . import resources
from .gameboy import Gameboy, RomLoadException

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
                    self.ui.system.gameboy.set_physical_cartridge()
                    self.ui.set_screen(GameScreen(self.ui))
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
    def __init__(self, ui: UI) -> None:
        self.ui = ui
        self.playing = True
        self._widget = SelectWidget(["Resume", "Reset", "Stats", "Main Menu"])

        self.ui.system.gameboy.reset()
        self.ui.system.gameboy.set_paused(False)

    def on_attach(self) -> None:
        self._render()

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
                    # Display Stats
                    self.ui.set_screen(StatsScreen(self.ui, self))
                    return
                if self._widget.pos == 3:
                    # Main Menu
                    self.ui.set_screen(MainMenuScreen(self.ui))
                    return
            self._render()

    def _render(self) -> None:
        if self.playing:
            return
        self.ui.draw.rectangle([(0, 0), (self.ui.width, self.ui.height)], fill=COLOR_TRANSPARENT)
        self.ui.draw.rectangle([(30, 30), (130, 144 - 30)], fill=COLOR_BG)
        self.ui.draw.rectangle([(30, 30), (130, 144 - 30)], outline=COLOR_BLACK)
        self._widget.render(self.ui, 40, 40, 80, 70)
        self.ui.show_framebuffer()


class RomSelectScreen(Screen):
    list_pos = 0

    def __init__(self, ui: UI) -> None:
        self.ui = ui
        rom_directory = self.ui.system.rom_directory
        self.roms = []
        self.roms.extend(rom_directory.glob("*.gb"))
        self.roms.extend(rom_directory.glob("*.gbc"))
        self.roms.sort()
        rom_filenames = [str(x.relative_to(rom_directory)) for x in self.roms]
        self._widget = ListWidget(rom_filenames, lines=9)
        widget_pos = min(RomSelectScreen.list_pos, len(rom_filenames) - 1)
        for i in range(widget_pos):
            self._widget.move_down()
        self._error = None

    def on_attach(self) -> None:
        self._render()

    def on_button_event(self, button: Button, event: ButtonEvent) -> None:
        if event == ButtonEvent.PRESSED:
            if self._error is not None:
                self._error = None
                self._render()
                return

            if button == Button.UP:
                self._widget.move_up()

            if button == Button.DOWN:
                self._widget.move_down()

            if button == Button.B:
                RomSelectScreen.list_pos = self._widget.pos
                self.ui.set_screen(MainMenuScreen(self.ui))
                return

            if button == Button.A:
                RomSelectScreen.list_pos = self._widget.pos
                rom_path = self.roms[self._widget.pos]
                try:
                    self.ui.system.gameboy.set_emulated_cartridge(rom_path)
                except RomLoadException as e:
                    self._error = str(e)
                    return
                self.ui.set_screen(GameScreen(self.ui))
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

        # Draw error modal
        if self._error is not None:
            bbox = self.ui.draw.textbbox((0, 0), self._error)
            text_w = bbox[2]
            text_h = bbox[3]
            draw_x = (160 // 2) - (text_w // 2)
            draw_y = (144 // 2) - (text_h // 2)
            self.ui.draw.rectangle(
                [draw_x - 4, draw_y - 4, draw_x + text_w + 4, draw_y + text_h + 4],
                outline=COLOR_BLACK, fill=COLOR_WHITE)
            self.ui.draw.text((draw_x, draw_y), self._error, fill=COLOR_BLACK)

        self.ui.show_framebuffer()


class StatsScreen(Screen):
    def __init__(self, ui: UI, prev_screen: Screen) -> None:
        self.ui = ui
        self._prev_screen = prev_screen

    def on_attach(self) -> None:
        self._render()

    def on_button_event(self, button: Button, event: ButtonEvent) -> None:
        if event == ButtonEvent.PRESSED:
            if button == Button.B:
                self.ui.set_screen(self._prev_screen)
                return

    def _get_stats(self) -> List[str]:
        # The 'clocks' stat register overflows in 8.53 minutes, so estimate it instead.
        clocks = self.ui.system.gameboy.get_playtime() * Gameboy.CLOCK_RATE
        stats = self.ui.system.gameboy.get_stats()
        stall_rate = stats['stalls'] / (clocks + 1)
        hit_rate = stats['cache_hits'] / (stats['cache_misses'] + stats['cache_hits'] + 1)
        return [
            f"Clocks: {int(clocks):,}",
            f"Stalls: {stats['stalls']:,}",
            f"Stall %: {(stall_rate * 100):0.3f}",
            f"Cache Hit %: {(hit_rate * 100):0.3f}",
        ]

    def _render(self) -> None:
        self.ui.draw.rectangle([(0, 0), (self.ui.width, self.ui.height)], fill=COLOR_TRANSPARENT)
        self.ui.draw.rectangle([(20, 20), (160 - 20, 144 - 20)], fill=COLOR_BG)
        self.ui.draw.rectangle([(20, 20), (160 - 20, 144 - 20)], outline=COLOR_BLACK)
        self.ui.draw.text(
            (28, 28),
            "Stats",
            fill=COLOR_BLACK,
            font=self.ui.font_bold,
        )
        self.ui.draw.multiline_text(
            (28, 28 + 14),
            "\n".join(self._get_stats()),
            fill=COLOR_BLACK,
        )
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
        else:
            # Wrap to the bottom
            self.pos = len(self.items) - 1
            self.start = max(0, len(self.items) - self.lines)

    def move_down(self) -> None:
        if self.pos < len(self.items) - 1:
            self.pos += 1
            if self.pos >= self.start + self.lines:
                self.start += 1
        else:
            # Wrap to the top
            self.pos = 0
            self.start = 0

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

