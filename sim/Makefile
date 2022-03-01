CXX	:= g++
OBJDIR  := obj_pc
RTLD	:= ../rtl
ifeq ($(VERILATOR_ROOT),)
VERILATOR_ROOT ?= $(shell bash -c 'verilator -V|grep VERILATOR_ROOT | head -1 | sed -e " s/^.*=\s*//"')
endif
VROOT   := $(VERILATOR_ROOT)
FLAGS	:= -Wall -Og -g -faligned-new -std=c++11
VINCD   := $(VROOT)/include
INCS	:= -I$(RTLD)/obj_dir/ -I$(RTLD) -I$(VINCD) -I$(VINCD)/vltstd
OBJS    := $(OBJDIR)/main.o
VOBJDR	:= $(RTLD)/obj_dir
VOBJS   := $(OBJDIR)/verilated.o $(OBJDIR)/verilated_vcd_c.o
PROGRAMS := main
all:	$(PROGRAMS)

%.o: $(OBJDIR)/%.o
$(OBJDIR)/%.o: %.cpp
	$(mk-objdir)
	$(CXX) $(FLAGS) $(INCS) -c $< -o $@

$(OBJDIR)/%.o: $(VINCD)/%.cpp
	$(mk-objdir)
	$(CXX) $(FLAGS) $(INCS) -c $< -o $@

main: $(OBJDIR)/main.o $(OBJS)
main: $(VOBJS) $(VOBJDR)/Vcpu__ALL.a
	$(CXX) -L . -l gbit $(INCS) $^ $(VOBJDR)/Vcpu__ALL.a -o $@

define	mk-objdir
	@bash -c "if [ ! -e $(OBJDIR) ]; then mkdir -p $(OBJDIR); fi"
endef

.PHONY: clean
clean:
	rm -f *.vcd
	rm -rf $(OBJDIR)/
	rm -f $(PROGRAMS)
	rm -f *.o