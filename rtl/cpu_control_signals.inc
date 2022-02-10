0: begin
    // NOP
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_write_enable = 0;
end
1: begin
    // INVALID
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    mem_write = 0;
    microbranch = MicroBranchJump;
    reg_write_enable = 0;
    next_state = 1;
end
2: begin
    // HALT
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    mem_write = 0;
    microbranch = MicroBranchJump;
    reg_write_enable = 0;
    next_state = 2;
end
3: begin
    // LD r, (HL)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelHl;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelReg8Dest;
    reg_write_enable = 1;
    reg_write_input = RegInputMem;
end
4: begin
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_write_enable = 0;
end
5: begin
    // LD (HL), r
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelHl;
    microbranch = MicroBranchNext;
    reg_read1_sel = RegSelReg8Src;
    reg_write_enable = 0;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelAReg1;
end
6: begin
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_write_enable = 0;
end
7: begin
    // LD r,r
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_read1_sel = RegSelReg8Src;
    reg_write_sel = RegSelReg8Dest;
    reg_write_enable = 1;
    reg_write_input = RegInputAlu;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelAReg1;
end
8: begin
    // LD r,n
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelReg8Dest;
    reg_write_enable = 1;
    reg_write_input = RegInputMem;
end
9: begin
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_write_enable = 0;
end
