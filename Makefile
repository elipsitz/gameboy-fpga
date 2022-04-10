sim: rtl
	make -C sim

rtl:
	cd rtl; $(MAKE) --no-print-directory

test: rtl
	make -C test

clean:
	cd rtl; $(MAKE) --no-print-directory clean
	cd test; $(MAKE) --no-print-directory clean

.PHONY: sim rtl test clean