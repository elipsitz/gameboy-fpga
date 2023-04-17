BUILD_DIR := ../build
VERILATOR := verilator
VFLAGS := -Wall -Wno-fatal -MMD -cc
CXX	:= g++
OBJDIR  := $(BUILD_DIR)
ifeq ($(VERILATOR_ROOT),)
VERILATOR_ROOT ?= $(shell bash -c 'verilator -V|grep VERILATOR_ROOT | head -1 | sed -e " s/^.*=\s*//"')
endif
VROOT   := $(VERILATOR_ROOT)
VINCD   := $(VROOT)/include
CFLAGS	:= -Wall -Og -g -faligned-new -std=c++20
INCS	:= -I$(BUILD_DIR)/ -I$(VINCD) -I$(VINCD)/vltstd
OBJS    := $(OBJDIR)/simulator.o $(OBJDIR)/window.o $(OBJDIR)/cartridge.o $(OBJDIR)/audio.o
VOBJS   := $(OBJDIR)/verilated.o $(OBJDIR)/verilated_vcd_c.o $(OBJDIR)/verilated_threads.o
PROGRAMS := $(BUILD_DIR)/simulator
all:	$(PROGRAMS)

$(BUILD_DIR)/V%__ALL.a: $(BUILD_DIR)/%.v
	$(SUBMAKE) verilator --cc $(BUILD_DIR)/$*.v --build --Mdir $(BUILD_DIR) -Wno-width

%.o: $(OBJDIR)/%.o
$(OBJDIR)/%.o: %.cpp
	$(mk-objdir)
	$(CXX) $(CFLAGS) $(INCS) `pkg-config sdl2 --cflags` -c $< -o $@

$(OBJDIR)/%.o: $(VINCD)/%.cpp
	$(mk-objdir)
	$(CXX) $(CFLAGS) $(INCS) `pkg-config sdl2 --cflags` -c $< -o $@

$(BUILD_DIR)/simulator: $(OBJDIR)/main.o $(OBJS)
$(BUILD_DIR)/simulator: $(BUILD_DIR)/VGameboy__ALL.a $(VOBJS)
	$(CXX) $(CFLAGS) $(INCS) `pkg-config sdl2 --cflags --libs` $^ -o $@

define	mk-objdir
	@bash -c "if [ ! -e $(OBJDIR) ]; then mkdir -p $(OBJDIR); fi"
endef

.PHONY: clean
clean:
	rm -rf $(BUILD_DIR)
	rm -f $(PROGRAMS)
