0: begin
    // NOP
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_op = RegOpNone;
    alu_write_flags = 0;
end
1: begin
    // _INVALID
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchJump;
    reg_op = RegOpNone;
    alu_write_flags = 0;
    next_state = 1;
end
2: begin
    // HALT
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchJump;
    reg_op = RegOpNone;
    alu_write_flags = 0;
    next_state = 2;
end
3: begin
    // LD r, (HL)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelHl;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelReg8Dest;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
    next_state = 0;
end
4: begin
    // LD (HL), r
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelHl;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelReg8Src;
    reg_op = RegOpNone;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_write_flags = 0;
    next_state = 0;
end
5: begin
    // LD (HL),n
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
6: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelHl;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelW;
    reg_op = RegOpNone;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_write_flags = 0;
    next_state = 0;
end
7: begin
    // LD r,r
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_read2_sel = RegSelReg8Src;
    reg_write_sel = RegSelReg8Dest;
    reg_op = RegOpWriteAlu;
    alu_op = AluOpCopyB;
    alu_sel_b = AluSelBReg2;
    alu_write_flags = 0;
end
8: begin
    // LD r,n
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelReg8Dest;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
    next_state = 0;
end
9: begin
    // LD rr,nn
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelReg16Lo;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
10: begin
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchJump;
    reg_write_sel = RegSelReg16Hi;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
    next_state = 0;
end
11: begin
    // ldi (HL),A
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelHl;
    microbranch = MicroBranchJump;
    reg_op = RegOpIncHl;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_write_flags = 0;
    next_state = 0;
end
12: begin
    // ldi A,(HL)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelHl;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
13: begin
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_op = RegOpIncHl;
    alu_write_flags = 0;
end
14: begin
    // ldd (HL),A
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelHl;
    microbranch = MicroBranchJump;
    reg_op = RegOpDecHl;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_write_flags = 0;
    next_state = 0;
end
15: begin
    // ldd A,(HL)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelHl;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
16: begin
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_op = RegOpDecHl;
    alu_write_flags = 0;
end
17: begin
    // ALU A, n
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
18: begin
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteAlu;
    alu_op = AluOpInstAlu;
    alu_sel_a = AluSelARegA;
    alu_sel_b = AluSelBReg2;
    alu_write_flags = 1;
end
19: begin
    // ALU A, (HL)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelHl;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
20: begin
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteAlu;
    alu_op = AluOpInstAlu;
    alu_sel_a = AluSelARegA;
    alu_sel_b = AluSelBReg2;
    alu_write_flags = 1;
end
21: begin
    // ALU A, r
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_read2_sel = RegSelReg8Src;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteAlu;
    alu_op = AluOpInstAlu;
    alu_sel_a = AluSelARegA;
    alu_sel_b = AluSelBReg2;
    alu_write_flags = 1;
end
22: begin
    // JP nn
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelZ;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
23: begin
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
24: begin
    // #PC=WZ
    pc_next = PcNextReg;
    inst_load = 0;
    mem_enable = 0;
    microbranch = MicroBranchJump;
    reg_read1_sel = RegSelW;
    reg_read2_sel = RegSelZ;
    reg_op = RegOpNone;
    alu_write_flags = 0;
    next_state = 0;
end
25: begin
    // JP HL
    pc_next = PcNextRegInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelHl;
    microbranch = MicroBranchDispatch;
    reg_read1_sel = RegSelHL;
    reg_read2_sel = RegSelHL;
    reg_op = RegOpNone;
    alu_write_flags = 0;
end
26: begin
    // JP f,nn
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelZ;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
27: begin
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchCond;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
    next_state = 24;
end
28: begin
    pc_next = PcNextInc;
    inst_load = 1;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchDispatch;
    reg_op = RegOpNone;
    alu_write_flags = 0;
end
29: begin
    // LD (rr), A
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelReg;
    microbranch = MicroBranchJump;
    reg_read1_sel = RegSelReg16Hi;
    reg_read2_sel = RegSelReg16Lo;
    reg_op = RegOpNone;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_write_flags = 0;
    next_state = 0;
end
30: begin
    // LD A, (rr)
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelReg;
    microbranch = MicroBranchJump;
    reg_read1_sel = RegSelReg16Hi;
    reg_read2_sel = RegSelReg16Lo;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
    next_state = 0;
end
31: begin
    // LD (nn), A
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelZ;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
32: begin
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
33: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelReg;
    microbranch = MicroBranchJump;
    reg_read1_sel = RegSelW;
    reg_read2_sel = RegSelZ;
    reg_op = RegOpNone;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_write_flags = 0;
    next_state = 0;
end
34: begin
    // LD A, (nn)
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelZ;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
35: begin
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
36: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelReg;
    microbranch = MicroBranchJump;
    reg_read1_sel = RegSelW;
    reg_read2_sel = RegSelZ;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
    next_state = 0;
end
37: begin
    // LD (FF00 + n), A
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
38: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelHigh;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelW;
    reg_op = RegOpNone;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_write_flags = 0;
    next_state = 0;
end
39: begin
    // LD (FF00 + C), A
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 1;
    mem_addr_sel = MemAddrSelHigh;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelC;
    reg_op = RegOpNone;
    alu_op = AluOpCopyA;
    alu_sel_a = AluSelARegA;
    alu_write_flags = 0;
    next_state = 0;
end
40: begin
    // LD A, (FF00 + n)
    pc_next = PcNextInc;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelPc;
    microbranch = MicroBranchNext;
    reg_write_sel = RegSelW;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
end
41: begin
    pc_next = PcNextSame;
    inst_load = 0;
    mem_enable = 1;
    mem_write = 0;
    mem_addr_sel = MemAddrSelHigh;
    microbranch = MicroBranchJump;
    reg_read2_sel = RegSelW;
    reg_write_sel = RegSelA;
    reg_op = RegOpWriteMem;
    alu_write_flags = 0;
    next_state = 0;
end
42: begin
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
    alu_write_flags = 0;
    next_state = 0;
end
