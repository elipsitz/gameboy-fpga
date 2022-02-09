0: begin
    // NOP
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    microbranch = MicroBranchDispatch;
    reg_write_input = 0;
end
1: begin
    // INVALID
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    mem_write = 0;
    microbranch = MicroBranchJump;
    reg_write_input = 0;
    next_state = 1;
end
2: begin
    // LD_r8_u8
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelReg8Dest;
    reg_write_enable = RegInputMem;
    reg_write_input = 1;
end
3: begin
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    microbranch = MicroBranchDispatch;
    reg_write_input = 0;
end
