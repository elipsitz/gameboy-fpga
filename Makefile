rtl:
	cd rtl; $(MAKE) --no-print-directory

test: rtl
	make -C test

clean:
	cd rtl; $(MAKE) --no-print-directory clean
	cd test; $(MAKE) --no-print-directory clean

.PHONY: rtl test clean