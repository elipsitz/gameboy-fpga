test: microcode
	make -C test

microcode:
	python3 rtl/gen_microcode.py

clean:
	rm -f rtl/cpu_control_*.inc
	rm -f test/*.gb

.PHONY: test microcode clean