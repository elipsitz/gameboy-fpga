VERILATOR := verilator
VOBJ := obj_dir
FBDIR := .
VFLAGS := -Wall -Wno-fatal -MMD --trace -cc 
SUBMAKE := $(MAKE) --no-print-directory --directory=$(VOBJ) -f

SRC := cpu.sv cpu_control.sv cpu_control_dispatch.inc cpu_control_signals.inc alu.sv

$(VOBJ)/Vcpu__ALL.a: $(VOBJ)/Vcpu.cpp $(VOBJ)/Vcpu.h
$(VOBJ)/Vcpu__ALL.a: $(VOBJ)/Vcpu.mk
$(VOBJ)/Vcpu.h $(VOBJ)/Vcpu.cpp $(VOBJ)/Vcpu.mk: $(SRC)

$(VOBJ)/V%.cpp $(VOBJ)/V%.h $(VOBJ)/V%.mk: $(FBDIR)/%.sv
	$(VERILATOR) $(VFLAGS) $*.sv

$(VOBJ)/V%.cpp: $(VOBJ)/V%.h
$(VOBJ)/V%.mk:  $(VOBJ)/V%.h
$(VOBJ)/V%.h: $(FBDIR)/%.sv

$(VOBJ)/V%__ALL.a: $(VOBJ)/V%.mk
	$(SUBMAKE) V$*.mk

cpu_control_dispatch.inc cpu_control_signals.inc: microcode.csv gen_microcode.py
	python3 gen_microcode.py

clean:
	rm -rf $(VOBJ)/
	rm -f *.inc

cpu: $(VOBJ)/Vcpu__ALL.a

all: cpu

.PHONY: all clean