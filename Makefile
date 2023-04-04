BUILD_DIR := build
export BUILD_DIR

sim: rtl
	make -C sim

rtl: $(BUILD_DIR)/Gameboy.v

# Compile Chisel to Verilog
$(BUILD_DIR)/Gameboy.v: .FORCE
	sbt "run --target-dir $(BUILD_DIR)"

test:
	sbt test

clean:
	rm -rf $(BUILD_DIR)

.PHONY: sim rtl test clean .FORCE

.FORCE: