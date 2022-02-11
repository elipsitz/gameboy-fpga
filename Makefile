test: microcode
	make -C test

microcode:
	python3 rtl/gen_microcode.py

.PHONY: test microcode