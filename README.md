# FPGA Game Boy Emulator

This repository contains a Game Boy / Game Boy Color emulator, written in [Chisel](https://www.chisel-lang.org/), that can be run on an FPGA. Read the full writeup [here](https://eli.lipsitz.net/posts/fpga-gameboy-emulator/).

<img src="https://raw.githubusercontent.com/elipsitz/gameboy-fpga/main/extra/device.jpg" align="center" height="400">

Currently, only the Pynq-Z2 is supported, although the core of the emulator should be able to be used on any FPGA with sufficient resources.

## Building

A precompiled bitstream is available under Releases.

### Setup

Currently this project uses Vivado 2020.2.

After installing, set up a shared IP cache to speed up synthesis of Xilinx IP:
1. Create the `~/vivado_ip_cache` directory (could be located somewhere else).
2. Create the file `~/.Xilinx/Vivado/Vivado_init.tcl`
3. In that file, add `set_param project.defaultIPCacheSetting /home/eli/vivado_ip_cache/` (substituting the absolute path to the IP cache directory).

### Running `fusesoc`

From the top-level directory:

```
fusesoc --cores-root . run --build --target=pynq_z2 elipsitz:gameboy:gameboy
```

This will generate a `.bit` and `.hwh` file, which need to be accessible to the PS Python code.

## Running

### Setup

Install Pynq Linux on the Pynq-Z2, and install the pip packages listed in `python/requirements.txt`.

Transfer the `gameboy_ps` Python module (located in the `python` directory) to the Pynq-Z2.

Place the `gameboy.bit` and `gameboy.hwh` files from the build process in the `gameboy_ps/resources` directory.

### HDMI and Controller

Connect an HDMI cable from the "HDMI OUT" port on the Pynq-Z2 to a monitor or TV.

Attach an Xbox-compatible controller to the USB port of the Pynq-Z2.

### Running 

As root, run `python3 -m gameboy_ps <path to ROM directory>`, passing the path to the directory containing ROM files.

The program will load the bitstream to the PL. It takes a few seconds to load all of the Pynq libraries, but the main menu should soon show up on the display.

Warning: the audio output can be quite loud. Start at the lowest setting on the monitor/TV and increase it as needed.

## Appendix

### Getting Xbox controller to work on Pynq-Z2

This needs the `xpad` and `joydev` drivers. The Pynq image doesn't come with it built,
so we'll need to build them as loadable kernel modules.

On the Pynq-Z2, go to `/lib/modules/5.15.19-xilinx-v2022.1/build`.

Download the right version of [`xpad.c`](https://raw.githubusercontent.com/torvalds/linux/v5.15/drivers/input/joystick/xpad.c)
to `drivers/input/joystick/xpad.c`.

And do the same with [`joydev.c`](https://raw.githubusercontent.com/torvalds/linux/v5.15/drivers/input/joydev.c), to `drivers/input/joydev.c`.

The build commands in `/lib/modules` need to run with `sudo` due to permissions issues.

Do `sudo make menuconfig` and set:

* `Device Drivers --> Input device support --> Joystick interface` to `M` (for 'module')
* `Device Drivers --> Input device support --> Joysticks/gamepads --> X-box Gamepad support` to `M` (for 'module')

Save and exit.

Then build the modules:

```
sudo make scripts
sudo make distclean
sudo make prepare
sudo make modules_prepare

sudo make -C . M=drivers/input/ joydev.ko
sudo make -C . M=drivers/input/joystick xpad.ko
```

TODO: figure out how to get them to be loaded on boot
(or use `insmod <modules>.ko` to load once)

Make sure the `xilinx` user is in the group `input`:
`sudo usermod -a -G input xilinx` (and re-login)

Install the Python library with `sudo pip3 install xbox360controller`

Create the device nodes if needed: https://docs.kernel.org/input/joydev/joystick.html#device-nodes

```
cd /dev
rm js*
mkdir input
mknod input/js0 c 13 0
mknod input/js1 c 13 1
mknod input/js2 c 13 2
mknod input/js3 c 13 3
ln -s input/js0 js0
ln -s input/js1 js1
ln -s input/js2 js2
ln -s input/js3 js3
```

