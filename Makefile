BUILD_DIR := build
export BUILD_DIR

sim: rtl
	make -C sim

rtl: $(BUILD_DIR)/SimGameboy.v

# Compile Chisel to Verilog
$(BUILD_DIR)/SimGameboy.v: .FORCE
	sbt "runMain platform.SimGameboy --target-dir $(BUILD_DIR)"

test:
	sbt test

clean:
	rm -rf $(BUILD_DIR)

.PHONY: sim rtl test clean .FORCE

.FORCE: