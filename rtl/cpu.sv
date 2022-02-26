`timescale 1ns/1ns

/// Control signal: how PC should be updated on an M-cycle.
typedef enum logic [1:0] {
    /// Do not change PC.
    PcNextSame,
    /// PC = output of incrementer/decrementer
    PcNextIncOut,
    /// PC = computed reset address
    PcNextRstAddr
} pc_next_e;

/// Control signal: 8-bit register select.
typedef enum logic [3:0] {
    /// The accumulator register.
    RegSelA,
    /// The flags register.
    RegSelF,
    /// Register C.
    RegSelC,
    /// Temp register W.
    RegSelW,
    /// Temp register Z.
    RegSelZ,
    /// Register H.
    RegSelH,
    /// Register L.
    RegSelL,
    /// The 8-bit register denoted by bits 2:0.
    RegSelReg8Src,
    /// The 8-bit register denoted by bits 5:3.
    RegSelReg8Dest,
    /// The high part of the 16-bit register denoted by bits 5:4.
    RegSelReg16Hi,
    /// The low part of the 16-bit register denoted by bits 5:4.
    RegSelReg16Lo,
    /// High byte of SP register.
    RegSPHi,
    /// Low byte of SP register.
    RegSPLo,
    /// High byte of PC register.
    RegPCHi,
    /// Low byte of PC register.
    RegPCLo
} reg_sel_e;

/// Control signal: register write operation.
typedef enum logic [1:0] {
    /// Do not write a register.
    RegOpNone,
    /// Write the output of the ALU.
    RegOpWriteAlu,
    /// Write the memory data input.
    RegOpWriteMem
} reg_op_e;

/// Control signal: register incrementer operation.
typedef enum logic [1:0] {
    /// Do nothing.
    IncOpNone,
    /// Increment.
    IncOpInc,
    /// Decrement.
    IncOpDec,
    /// Increment but no writeback.
    IncOpIncNoWrite
} inc_op_e;

/// Control signal: register incrementer target selector.
typedef enum logic [2:0] {
    /// PC register
    IncRegPC,
    /// HL register
    IncRegHL,
    /// SP register
    IncRegSP,
    /// WZ register
    IncRegWZ,
    /// 16-bit register selected by instruction register
    IncRegInst16,
    /// Target: PC register, Input: {ALU Out, PC Lo} (used for relative jump).
    IncRegPC_ALU
} inc_reg_e;

/// Control signal: ALU operation.
typedef enum logic [3:0] {
    /// Output = A
    AluOpCopyA,
    /// Output = B
    AluOpCopyB,
    /// Output = B + 1
    AluOpIncB,
    /// Output = B - 1
    AluOpDecB,
    /// Output = A + B (and set the internal carry flag)
    AluOpAddLo,
    /// Output = A + B (and use the internal carry flag)
    AluOpAddHi,
    /// Use the "ALU opcode" from the instruction (ADD/ADC/SUB/SBC/AND/XOR/OR/CP).
    AluOpInstAlu,
    /// Use the "Acc/Flag opcode" from the instruction (RLCA/RRCA/RLA/RRA/DAA/CPL/SCF/CCF).
    AluOpInstAcc,
    /// Use the CB-prefixed opcode from the instruction.
    AluOpInstCB,
    /// CB-prefixed single bit operations (get/reset/set)
    AluOpInstBit
} alu_op_e;

/// Control signal: ALU operand A source.
typedef enum logic [0:0] {
    /// A = Accumulator register
    AluSelARegA,
    /// A = Register Read 1,
    AluSelAReg1
} alu_sel_a_e;

/// Control signal: ALU operand B source.
typedef enum logic [0:0] {
    /// B = Register Read 2
    AluSelBReg2,
    /// B = Sign extension of Register Read 2
    AluSelBSignReg2
} alu_sel_b_e;

/// Control signal: ALU flag set mode.
typedef enum logic [2:0] {
    /// F = ---- (no change)
    AluFlagSetNone,
    /// F = **** (all set)
    AluFlagSetAll,
    /// F = -*** (all set except zero)
    AluFlagSet_NHC,
    /// F = 0*** (all set, zero unset)
    AluFlagSet0NHC
} alu_flag_set_e;

/// Control signal: where the memory load/store address comes from.
typedef enum logic [1:0] {
    /// **INPUT** to the register incrementer (not output).
    MemAddrSelIncrementer,
    /// High address: 0xFF00 | (Register 2)
    MemAddrSelHigh
} mem_addr_sel_e;

/// GameBoy CPU - Sharp SM83
module cpu (
    /// Clock (normally 4 MHz)
    input clk,
    /// Synchronous reset signal (active high)
    input reset,

    /// System bus address selection.
    output logic [15:0] mem_addr,
    /// System bus access enable.
    output logic        mem_enable,
    /// System bus write enable.
    output logic        mem_write,
    /// System bus data in.
    input        [7:0]  mem_data_in,
    /// System bus data out.
    output logic [7:0]  mem_data_out
);
    //////////////////////////////////////// Clocking: T-Cycles
    logic [1:0] t_cycle = 0;
    logic clk_phi;
    assign clk_phi = !t_cycle[1];
    always_ff @(posedge clk) begin
        if (reset) t_cycle <= 0;
        else t_cycle <= t_cycle + 1;
    end

    //////////////////////////////////////// Control Unit
    logic condition;
    pc_next_e pc_next;
    logic inst_load;
    reg_sel_e reg_read1_sel;
    reg_sel_e reg_read2_sel;
    reg_sel_e reg_write_sel;
    reg_op_e reg_op;
    inc_op_e inc_op;
    inc_reg_e inc_reg;
    alu_op_e alu_op;
    alu_sel_a_e alu_sel_a;
    alu_sel_b_e alu_sel_b;
    mem_addr_sel_e mem_addr_sel;
    alu_flag_set_e alu_flag_set;
    // Holds the current instruction. Used to address registers, etc.
    logic [7:0] instruction_register = 0;
    cpu_control control (
        .clk,
        .t_cycle,
        .reset,
        .mem_data_in,
        .condition,
        .pc_next,
        .inst_load,
        .reg_read1_sel,
        .reg_read2_sel,
        .reg_write_sel,
        .reg_op,
        .inc_op,
        .inc_reg,
        .alu_op,
        .alu_sel_a,
        .alu_sel_b,
        .alu_flag_set,
        .mem_enable,
        .mem_write,
        .mem_addr_sel
    );
    always_ff @(posedge clk) begin
        // Handle instruction register.
        if (reset) instruction_register <= 0;
        else if (t_cycle == 3 && inst_load) instruction_register <= mem_data_in;
    end

    //////////////////////////////////////// ALU
    localparam FLAG_C = 2'd0;
    localparam FLAG_H = 2'd1;
    localparam FLAG_N = 2'd2;
    localparam FLAG_Z = 2'd3;
    logic [3:0] flag_read;
    logic [7:0] alu_out;
    logic [7:0] alu_a;
    logic [7:0] alu_b;
    logic [3:0] alu_flag_in;
    logic [2:0] alu_bit_index;
    logic [3:0] alu_flag_out;
    logic [4:0] alu_inner_op;
    logic [3:0] alu_flag_next;
    logic alu_internal_carry;
    always @(*) begin
        case (alu_sel_a) 
            AluSelARegA: alu_a = registers[7];
            AluSelAReg1: alu_a = reg_read1_out;
        endcase

        case (alu_sel_b)
            AluSelBReg2: alu_b = reg_read2_out;
            AluSelBSignReg2: alu_b = {8{reg_read2_out[7]}};
        endcase

        case (alu_op)
            AluOpCopyA: alu_inner_op = 5'b11000;
            AluOpCopyB: alu_inner_op = 5'b11001;
            AluOpIncB: alu_inner_op = 5'b11010;
            AluOpDecB: alu_inner_op = 5'b11011;
            AluOpInstAlu: alu_inner_op = {2'b00, instruction_register[5:3]};
            AluOpInstAcc: alu_inner_op = {2'b01, instruction_register[5:3]};
            AluOpInstCB: alu_inner_op = {2'b10, instruction_register[5:3]};
            AluOpInstBit: alu_inner_op = {3'b111, instruction_register[7:6]};
            AluOpAddLo: alu_inner_op = 5'b00000; // ADD
            AluOpAddHi: alu_inner_op = 5'b00001; // ADC
        endcase

        case (alu_op)
            AluOpAddHi: alu_flag_in = {3'd0, alu_internal_carry};
            default: alu_flag_in = flag_read;
        endcase

        alu_bit_index = instruction_register[5:3];
    end
    always @(*) begin
        case (alu_flag_set)
            AluFlagSetNone: alu_flag_next = alu_flag_in;
            AluFlagSetAll: alu_flag_next = alu_flag_out;
            AluFlagSet_NHC: alu_flag_next = {alu_flag_in[3], alu_flag_out[2:0]}; 
            AluFlagSet0NHC: alu_flag_next = {1'b0, alu_flag_out[2:0]};
        endcase
    end
    always_ff @(posedge clk) begin
        if (t_cycle == 3) begin
            if (alu_op == AluOpAddLo) alu_internal_carry <= alu_flag_out[FLAG_C];
        end
    end
    alu alu (
        .alu_a,
        .alu_b,
        .alu_op(alu_inner_op),
        .alu_flag_in,
        .alu_bit_index,
        .alu_flag_out,
        .alu_out
    );

    //////////////////////////////////////// Register File
    // Includes incrementer/decrementer.
    // 14 Registers: BC DE HL FA SP WZ PC
    //               01 23 45 67 89 AB CD
    logic [15:0] pc; // Current PC.
    logic [7:0] registers [0:13];
    logic [7:0] reg_read1_out; // The value of selected Register 1.
    logic [7:0] reg_read2_out; // The value of selected Register 2.
    logic [3:0] reg_read1_index; // The index of the first register to read.
    logic [3:0] reg_read2_index; // The index of the second register to read.
    logic [3:0] reg_write_index; // The index of the register to write.
    logic [3:0] reg_r16_hi; // The instruction register r16 interpreted high.
    logic [3:0] reg_r16_lo; // The instruction register r16 interpreted low.
    logic [15:0] inc_in; // Input to the incrementer/decrementer.
    logic [15:0] inc_out; // Output of the incrementer/decrementer.
    assign pc = {registers[12], registers[13]};
    assign flag_read = registers[6][7:4];
    always @(*) begin
        case (instruction_register[5:4])
            2'b00: reg_r16_hi = 0;
            2'b01: reg_r16_hi = 2;
            2'b10: reg_r16_hi = 4;
            2'b11: reg_r16_hi = 8;
        endcase
        case (instruction_register[5:4])
            2'b00: reg_r16_lo = 1;
            2'b01: reg_r16_lo = 3;
            2'b10: reg_r16_lo = 5;
            2'b11: reg_r16_lo = 9;
        endcase

        case (reg_read1_sel)
            RegSelA: reg_read1_index = 7;
            RegSelF: reg_read1_index = 6;
            RegSelC: reg_read1_index = 1;
            RegSelW: reg_read1_index = 10;
            RegSelZ: reg_read1_index = 11;
            RegSelH: reg_read1_index = 4;
            RegSelL: reg_read1_index = 5;
            RegSelReg8Src: reg_read1_index = {1'b0, instruction_register[2:0]};
            RegSelReg8Dest: reg_read1_index = {1'b0, instruction_register[5:3]};
            RegSelReg16Hi: reg_read1_index = reg_r16_hi;
            RegSelReg16Lo: reg_read1_index = reg_r16_lo;
            RegSPHi: reg_read1_index = 8;
            RegSPLo: reg_read1_index = 9;
            RegPCHi: reg_read1_index = 12;
            RegPCLo: reg_read1_index = 13;
        endcase
        case (reg_read2_sel)
            RegSelA: reg_read2_index = 7;
            RegSelF: reg_read2_index = 6;
            RegSelC: reg_read2_index = 1;
            RegSelW: reg_read2_index = 10;
            RegSelZ: reg_read2_index = 11;
            RegSelH: reg_read2_index = 4;
            RegSelL: reg_read2_index = 5;
            RegSelReg8Src: reg_read2_index = {1'b0, instruction_register[2:0]};
            RegSelReg8Dest: reg_read2_index = {1'b0, instruction_register[5:3]};
            RegSelReg16Hi: reg_read2_index = reg_r16_hi;
            RegSelReg16Lo: reg_read2_index = reg_r16_lo;
            RegSPHi: reg_read2_index = 8;
            RegSPLo: reg_read2_index = 9;
            RegPCHi: reg_read2_index = 12;
            RegPCLo: reg_read2_index = 13;
        endcase
        case (reg_write_sel)
            RegSelA: reg_write_index = 7;
            RegSelF: reg_write_index = 6;
            RegSelC: reg_write_index = 1;
            RegSelW: reg_write_index = 10;
            RegSelZ: reg_write_index = 11;
            RegSelH: reg_write_index = 4;
            RegSelL: reg_write_index = 5;
            RegSelReg8Src: reg_write_index = {1'b0, instruction_register[2:0]};
            RegSelReg8Dest: reg_write_index = {1'b0, instruction_register[5:3]};
            RegSelReg16Hi: reg_write_index = reg_r16_hi;
            RegSelReg16Lo: reg_write_index = reg_r16_lo;
            RegSPHi: reg_write_index = 8;
            RegSPLo: reg_write_index = 9;
            RegPCHi: reg_write_index = 12;
            RegPCLo: reg_write_index = 13;
        endcase
        reg_read1_out = registers[reg_read1_index];
        reg_read2_out = registers[reg_read2_index];
    end
    always @(*) begin
        case (inc_reg)
            IncRegPC: inc_in = pc;
            IncRegHL: inc_in = {registers[4], registers[5]};
            IncRegSP: inc_in = {registers[8], registers[9]};
            IncRegWZ: inc_in = {registers[10], registers[11]};
            IncRegInst16: inc_in = {registers[reg_r16_hi], registers[reg_r16_lo]};
            IncRegPC_ALU: inc_in = {alu_out, pc[7:0]};
        endcase
        case (inc_op)
            IncOpNone: inc_out = inc_in;
            IncOpInc, IncOpIncNoWrite: inc_out = inc_in + 1;
            IncOpDec: inc_out = inc_in - 1;
        endcase
    end
    int i;
    initial begin
        for (i = 0; i < 14; i += 1) registers[i] = 0;
    end
    always_ff @(posedge clk) begin
        if (reset) begin
            for (i = 0; i < 14; i += 1) registers[i] <= 0;
        end else if (t_cycle == 3) begin
            case (reg_op)
                RegOpWriteAlu: registers[reg_write_index] <= alu_out;
                RegOpWriteMem: begin
                    // Keep bottom 4 bits of Flags register 0.
                    if (reg_write_index == 6) registers[reg_write_index] <= {mem_data_in[7:4], 4'd0};
                    else registers[reg_write_index] <= mem_data_in;
                end
            endcase
            if (inc_op == IncOpInc || inc_op == IncOpDec) begin
                case (inc_reg)
                    IncRegHL: {registers[4], registers[5]} <= inc_out;
                    IncRegSP: {registers[8], registers[9]} <= inc_out;
                    IncRegWZ: {registers[10], registers[11]} <= inc_out;
                    IncRegInst16: {registers[reg_r16_hi], registers[reg_r16_lo]} <= inc_out;
                endcase
            end
            case (pc_next)
                PcNextIncOut: {registers[12], registers[13]} <= inc_out;
                PcNextRstAddr: {registers[12], registers[13]} <= {10'd0, instruction_register[5:3], 3'd0};
            endcase
            if (alu_flag_set != AluFlagSetNone) registers[6] <= {alu_flag_next, 4'b0000};
        end
    end

    //////////////////////////////////////// Condition Code Checking
    always @(*) begin
        case (instruction_register[4:3])
            0: condition = !flag_read[FLAG_Z];
            1: condition = flag_read[FLAG_Z];
            2: condition = !flag_read[FLAG_C];
            3: condition = flag_read[FLAG_C];
        endcase
    end

    //////////////////////////////////////// Memory Accesses
    assign mem_data_out = alu_out; // TODO support other places?
    always_comb begin
        case (mem_addr_sel)
            MemAddrSelIncrementer: mem_addr = inc_in;
            MemAddrSelHigh: mem_addr = {8'hFF, reg_read2_out};
        endcase
    end

    //////////////////////////////////////// Misc.
    `ifdef COCOTB_SIM
    initial begin
        $dumpfile ("cpu.vcd");
        $dumpvars (0, cpu);
        #1;
    end
    `endif
endmodule