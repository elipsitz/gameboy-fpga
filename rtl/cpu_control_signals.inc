0: begin
    // NOP
    pc_next = PcNextIncOut;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchDispatch;
    reg_op = RegOpNone;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
1: begin
    // _INVALID
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchJump;
    reg_op = RegOpNone;
    inc_op = IncOpNone;
    alu_flag_set = AluFlagSetNone;
    next_state = 1;
end
2: begin
    // HALT
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchJump;
    reg_op = RegOpNone;
    inc_op = IncOpNone;
    alu_flag_set = AluFlagSetNone;
    next_state = 2;
end
3: begin
    // LD r, (HL)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelReg8Dest;
    reg_op = RegOpWriteMem;
    inc_op = IncOpNone;
    inc_reg = IncRegHL;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
4: begin
    // LD (HL), r
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelReg8Src;
    reg_op = RegOpNone;
    inc_op = IncOpNone;
    inc_reg = IncRegHL;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
5: begin
    // LD (HL),n
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
6: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelW;
    reg_op = RegOpNone;
    inc_op = IncOpNone;
    inc_reg = IncRegHL;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
7: begin
    // LD r,r
    pc_next = PcNextIncOut;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchDispatch;
    reg_read2_sel = RegSelReg8Src;
    reg_write_sel = RegSelReg8Dest;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
end
8: begin
    // LD r,n
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelReg8Dest;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
9: begin
    // LD rr,nn
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelReg16Lo;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
10: begin
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelReg16Hi;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
11: begin
    // ldi (HL),A
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_op = RegOpNone;
    inc_op = IncOpInc;
    inc_reg = IncRegHL;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
12: begin
    // ldi A,(HL)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegHL;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
13: begin
    // ldd (HL),A
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_op = RegOpNone;
    inc_op = IncOpDec;
    inc_reg = IncRegHL;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
14: begin
    // ldd A,(HL)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    inc_op = IncOpDec;
    inc_reg = IncRegHL;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
15: begin
    // ALU A, n
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
16: begin
    pc_next = PcNextIncOut;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchDispatch;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_op = AluOpInstAlu;
    alu_sel_a = AluSelARegA;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetAll;
end
17: begin
    // ALU A, (HL)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpNone;
    inc_reg = IncRegHL;
    alu_flag_set = AluFlagSetNone;
end
18: begin
    pc_next = PcNextIncOut;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchDispatch;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_op = AluOpInstAlu;
    alu_sel_a = AluSelARegA;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetAll;
end
19: begin
    // ALU A, r
    pc_next = PcNextIncOut;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchDispatch;
    reg_read2_sel = RegSelReg8Src;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_op = AluOpInstAlu;
    alu_sel_a = AluSelARegA;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetAll;
end
20: begin
    // JP nn
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelZ;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
21: begin
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
22: begin
    // #PC=WZ
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchJump;
    reg_op = RegOpNone;
    inc_op = IncOpNone;
    inc_reg = IncRegWZ;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
23: begin
    // JP HL
    pc_next = PcNextIncOut;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchDispatch;
    reg_op = RegOpNone;
    inc_op = IncOpIncNoWrite;
    inc_reg = IncRegHL;
    alu_flag_set = AluFlagSetNone;
end
24: begin
    // JP f,nn
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelZ;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
25: begin
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchCond;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
    next_state = 22;
end
26: begin
    pc_next = PcNextIncOut;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchDispatch;
    reg_op = RegOpNone;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
27: begin
    // LD (rr), A
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_op = RegOpNone;
    inc_op = IncOpNone;
    inc_reg = IncRegInst16;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
28: begin
    // LD A, (rr)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    inc_op = IncOpNone;
    inc_reg = IncRegInst16;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
29: begin
    // LD (nn), A
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelZ;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
30: begin
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
31: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_op = RegOpNone;
    inc_op = IncOpNone;
    inc_reg = IncRegWZ;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
32: begin
    // LD A, (nn)
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelZ;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
33: begin
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
34: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    inc_op = IncOpNone;
    inc_reg = IncRegWZ;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
35: begin
    // LD (FF00 + n), A
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
36: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelHigh;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelW;
    reg_op = RegOpNone;
    inc_op = IncOpNone;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
37: begin
    // LD (FF00 + C), A
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelHigh;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelC;
    reg_op = RegOpNone;
    inc_op = IncOpNone;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
38: begin
    // LD A, (FF00 + n)
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
39: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelHigh;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    inc_op = IncOpNone;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
40: begin
    // LD A, (FF00 + C)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelHigh;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelC;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    inc_op = IncOpNone;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
41: begin
    // INC rr
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchJump;
    reg_op = RegOpNone;
    inc_op = IncOpInc;
    inc_reg = IncRegInst16;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
42: begin
    // DEC rr
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchJump;
    reg_op = RegOpNone;
    inc_op = IncOpDec;
    inc_reg = IncRegInst16;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
43: begin
    // LD (nn), SP
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelZ;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
44: begin
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
45: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_read2_sel = RegSPLo;
    reg_op = RegOpNone;
    inc_op = IncOpInc;
    inc_reg = IncRegWZ;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
end
46: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSPHi;
    reg_op = RegOpNone;
    inc_op = IncOpInc;
    inc_reg = IncRegWZ;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
47: begin
    // LD SP, HL
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchNext;
    reg_read2_sel = RegSelL;
    reg_write_sel = RegSPLo;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpNone;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
end
48: begin
    pc_next = PcNextIncOut;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchDispatch;
    reg_read2_sel = RegSelH;
    reg_write_sel = RegSPHi;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
end
49: begin
    // PUSH AF
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    mem_write = 0;
    microbranch = MicroBranchNext;
    reg_op = RegOpNone;
    inc_op = IncOpDec;
    inc_reg = IncRegSP;
    alu_flag_set = AluFlagSetNone;
end
50: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_read2_sel = RegSelA;
    reg_op = RegOpNone;
    inc_op = IncOpDec;
    inc_reg = IncRegSP;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
end
51: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelF;
    reg_op = RegOpNone;
    inc_op = IncOpNone;
    inc_reg = IncRegSP;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
52: begin
    // POP AF
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelF;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegSP;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
end
53: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegSP;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
54: begin
    // PUSH rr
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    mem_write = 0;
    microbranch = MicroBranchNext;
    reg_op = RegOpNone;
    inc_op = IncOpDec;
    inc_reg = IncRegSP;
    alu_flag_set = AluFlagSetNone;
end
55: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_read2_sel = RegSelReg16Hi;
    reg_op = RegOpNone;
    inc_op = IncOpDec;
    inc_reg = IncRegSP;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
end
56: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelReg16Lo;
    reg_op = RegOpNone;
    inc_op = IncOpNone;
    inc_reg = IncRegSP;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
57: begin
    // POP rr
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelReg16Lo;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegSP;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
end
58: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelReg16Hi;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegSP;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
59: begin
    // ADD HL,rr
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchNext;
    reg_read1_sel = RegSelL;
    reg_read2_sel = RegSelReg16Lo;
    reg_write_sel = RegSelL;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpNone;
    alu_op = AluOpAddLo;
    alu_sel_a = AluSelAReg1;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
end
60: begin
    pc_next = PcNextIncOut;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchDispatch;
    reg_read1_sel = RegSelH;
    reg_read2_sel = RegSelReg16Hi;
    reg_write_sel = RegSelH;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_op = AluOpAddHi;
    alu_sel_a = AluSelAReg1;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSet_NHC;
end
61: begin
    // ADD SP,dd
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
62: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchNext;
    reg_read1_sel = RegSPLo;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegSPLo;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpNone;
    alu_op = AluOpAddLo;
    alu_sel_a = AluSelAReg1;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSet0NHC;
end
63: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchJump;
    reg_read1_sel = RegSPHi;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegSPHi;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpNone;
    alu_op = AluOpAddHi;
    alu_sel_a = AluSelAReg1;
    alu_sel_b = AluSelBSignReg2;
    alu_flag_set = AluFlagSetNone;
    next_state = 0;
end
64: begin
    // ld HL,SP+dd
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
65: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchNext;
    reg_read1_sel = RegSPLo;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegSelL;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpNone;
    alu_op = AluOpAddLo;
    alu_sel_a = AluSelAReg1;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSet0NHC;
end
66: begin
    pc_next = PcNextIncOut;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchDispatch;
    reg_read1_sel = RegSPHi;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegSelH;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_op = AluOpAddHi;
    alu_sel_a = AluSelAReg1;
    alu_sel_b = AluSelBSignReg2;
    alu_flag_set = AluFlagSetNone;
end
67: begin
    // JR e
    pc_next = PcNextIncOut;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    inc_op = IncOpInc;
    inc_reg = IncRegPC;
    alu_flag_set = AluFlagSetNone;
end
68: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchNext;
    reg_read1_sel = RegPCLo;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegPCLo;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpNone;
    alu_op = AluOpAddLo;
    alu_sel_a = AluSelAReg1;
    alu_sel_b = AluSelBReg2;
    alu_flag_set = AluFlagSetNone;
end
69: begin
    pc_next = PcNextIncOut;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelIncrementer;
    microbranch = MicroBranchDispatch;
    reg_read1_sel = RegPCHi;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegPCHi;
    reg_op = RegOpWriteAlu;
    inc_op = IncOpIncNoWrite;
    inc_reg = IncRegPC_ALU;
    alu_op = AluOpAddHi;
    alu_sel_a = AluSelAReg1;
    alu_sel_b = AluSelBSignReg2;
    alu_flag_set = AluFlagSetNone;
end
