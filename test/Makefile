# Test Bench Makefile

# defaults
SIM ?= icarus
TOPLEVEL_LANG ?= verilog

COMPILE_ARGS = -grelative-include

VERILOG_SOURCES += $(PWD)/../rtl/alu.sv
VERILOG_SOURCES += $(PWD)/../rtl/cpu.sv
VERILOG_SOURCES += $(PWD)/../rtl/cpu_control.sv

CUSTOM_COMPILE_DEPS += $(PWD)/../rtl/cpu_control_signals.inc
CUSTOM_COMPILE_DEPS += $(PWD)/../rtl/cpu_control_dispatch.inc
CUSTOM_COMPILE_DEPS += basic_test.gb
CUSTOM_COMPILE_DEPS += sanity_test.gb
CUSTOM_COMPILE_DEPS += complete_test.gb

# TOPLEVEL is the name of the toplevel module in your Verilog or VHDL file
TOPLEVEL = cpu

# MODULE is the basename of the Python test file
MODULE = test_cpu

# include cocotb's make rules to take care of the simulator setup
include $(shell cocotb-config --makefiles)/Makefile.sim

%.gb : %.o
	rgblink -o $@ $<

%.o : %.s
	rgbasm -o $@ $<
