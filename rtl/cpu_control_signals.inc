0: begin
    // NOP
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
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
    // LD r,r
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    microbranch = MicroBranchDispatch;
    reg_read1_sel = RegSelReg8Src;
    reg_write_sel = RegSelReg8Dest;
    reg_write_enable = 1;
    reg_write_input = RegInputAlu;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelAReg1;
end
4: begin
    // LD r,n
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelReg8Dest;
    reg_write_enable = 1;
    reg_write_input = RegInputMem;
end
5: begin
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    microbranch = MicroBranchDispatch;
    reg_write_enable = 0;
end
