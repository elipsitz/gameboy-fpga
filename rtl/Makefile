VERILATOR := verilator
VOBJ := obj_dir
FBDIR := .
VFLAGS := -Wall -Wno-fatal -MMD --trace -cc -Icpu
SUBMAKE := $(MAKE) --no-print-directory --directory=$(VOBJ) -f

SRC := gameboy.sv cpu.sv ppu.sv cpu/cpu_control.sv cpu/cpu_control_dispatch.inc cpu/cpu_control_signals.inc cpu/alu.sv

$(VOBJ)/V%.cpp $(VOBJ)/V%.h $(VOBJ)/V%.mk: $(FBDIR)/%.sv $(SRC)
	$(VERILATOR) $(VFLAGS) $*.sv

$(VOBJ)/V%.cpp: $(VOBJ)/V%.h
$(VOBJ)/V%.mk:  $(VOBJ)/V%.h
$(VOBJ)/V%.h: $(FBDIR)/%.sv

$(VOBJ)/V%__ALL.a: $(VOBJ)/V%.mk $(VOBJ)/V%.cpp $(VOBJ)/V%.h
	$(SUBMAKE) V$*.mk

all: gameboy

cpu/cpu_control_dispatch.inc cpu/cpu_control_signals.inc: cpu/microcode.csv cpu/gen_microcode.py
	cd cpu; python3 gen_microcode.py

clean:
	rm -rf $(VOBJ)/
	rm -f *.inc

cpu: $(VOBJ)/Vcpu__ALL.a
gameboy: $(VOBJ)/Vgameboy__ALL.a

.PHONY: all clean gameboy cpu