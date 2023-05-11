## Getting Xbox controller to work on Pynq-Z2

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
