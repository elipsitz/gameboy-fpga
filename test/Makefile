# Test Bench Makefile

# defaults
SIM ?= icarus
TOPLEVEL_LANG ?= verilog

COMPILE_ARGS = -grelative-include

VERILOG_SOURCES += $(PWD)/../rtl/cpu.sv
VERILOG_SOURCES += $(PWD)/../rtl/cpu_control.sv

# TOPLEVEL is the name of the toplevel module in your Verilog or VHDL file
TOPLEVEL = cpu

# MODULE is the basename of the Python test file
MODULE = test_cpu

# include cocotb's make rules to take care of the simulator setup
include $(shell cocotb-config --makefiles)/Makefile.sim