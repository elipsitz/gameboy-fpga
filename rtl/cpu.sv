`timescale 1ns/1ns

/// Control signal: how PC should be updated on an M-cycle.
typedef enum logic [0:0] {
    /// Do not change PC.
    PcNextSame,
    /// Increment PC by 1.
    PcNextInc
} pc_next_e;

/// Control signal: 8-bit register select.
typedef enum logic [1:0] {
    /// The accumulator register.
    RegSelA,
    /// The 8-bit register denoted by bits 2:0.
    RegSelReg8Src,
    /// The 8-bit register denoted by bits 5:3.
    RegSelReg8Dest
} reg_sel_e;

/// Control signal: register write data source.
typedef enum logic [0:0] {
    /// The output of the ALU.
    RegInputAlu,
    /// The memory data input.
    RegInputMem
} reg_input_e;

/// Control signal: ALU operation.
typedef enum logic [0:0] {
    /// Output = A
    AluOpCopyA,
    /// Output = A + 1
    AluOpIncA
} alu_op_e;

/// Control signal: ALU operand A source.
typedef enum logic [0:0] {
    /// A = Register Read 1
    AluSelAReg1
} alu_sel_a_e;

/// Control signal: ALU operand B source.
typedef enum logic [0:0] {
    /// B = Register Read 2
    AluSelBReg2
} alu_sel_b_e;

/// GameBoy CPU - Sharp SM83
module cpu (
    /// Clock (normally 4 MHz)
    input clk,
    /// Synchronous reset signal (active high)
    input reset,

    /// System bus address selection.
    output [15:0] mem_addr,
    /// System bus access enable.
    output        mem_enable,
    /// System bus write enable.
    output        mem_write,
    /// System bus data in.
    input  [7:0]  mem_data_in,
    /// System bus data out.
    output [7:0]  mem_data_out
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
    logic pc_next;
    logic inst_load;
    reg_sel_e reg_read1_sel;
    reg_sel_e reg_read2_sel;
    reg_sel_e reg_write_sel;
    logic reg_write_enable;
    reg_input_e reg_write_input;
    alu_op_e alu_op;
    alu_sel_a_e alu_sel_a;
    alu_sel_b_e alu_sel_b;
    // Holds the current instruction. Used to address registers, etc.
    logic [7:0] instruction_register = 0;
    cpu_control control (
        .clk,
        .t_cycle,
        .reset,
        .mem_data_in,
        .pc_next,
        .inst_load,
        .reg_read1_sel,
        .reg_read2_sel,
        .reg_write_sel,
        .reg_write_enable,
        .reg_write_input,
        .alu_op,
        .alu_sel_a,
        .alu_sel_b,
        .mem_enable,
        .mem_write
    );
    always_ff @(posedge clk) begin
        // Handle instruction register.
        if (reset) instruction_register <= 0;
        else if (t_cycle == 3 && inst_load) instruction_register <= mem_data_in;
    end

    //////////////////////////////////////// Program Counter
    logic [15:0] pc = 0; // Current PC
    always_ff @(posedge clk) begin
        if (reset) pc <= 0;
        else if (t_cycle == 3) begin
            if (pc_next == PcNextInc) pc <= pc + 16'd1;
        end
    end

    //////////////////////////////////////// ALU
    logic [7:0] alu_out;
    logic [7:0] alu_input_a;
    logic [7:0] alu_input_b;
    always_comb begin
        // Select ALU input A.
        case (alu_sel_a) 
            AluSelAReg1: alu_input_a = reg_read1_out;
        endcase

        // Select ALU input B.
        case (alu_sel_b)
            AluSelBReg2: alu_input_b = reg_read2_out;
        endcase

        // Compute ALU output.
        case (alu_op)
            AluOpCopyA: alu_out = alu_input_a;
            AluOpIncA: alu_out = alu_input_a + 1;
        endcase
    end

    //////////////////////////////////////// Register File
    // 11 Registers: BC DE HL FA SP WZ
    //               01 23 45 67 89 AB
    logic [7:0] registers [0:11];
    logic [7:0] reg_read1_out; // The value of selected Register 1.
    logic [7:0] reg_read2_out; // The value of selected Register 2.
    logic [3:0] reg_read1_index; // The index of the first register to read.
    logic [3:0] reg_read2_index; // The index of the second register to read.
    logic [3:0] reg_write_index; // The index of the register to write.
    logic [7:0] reg_write_data; // The data to write to the register.
    always @(*) begin
        case (reg_read1_sel)
            RegSelA: reg_read1_index = 7;
            RegSelReg8Src: reg_read1_index = {1'b0, instruction_register[2:0]};
            RegSelReg8Dest: reg_read1_index = {1'b0, instruction_register[5:3]};
        endcase
        case (reg_read2_sel)
            RegSelA: reg_read2_index = 7;
            RegSelReg8Src: reg_read2_index = {1'b0, instruction_register[2:0]};
            RegSelReg8Dest: reg_read2_index = {1'b0, instruction_register[5:3]};
        endcase
        case (reg_write_sel)
            RegSelA: reg_write_index = 7;
            RegSelReg8Src: reg_write_index = {1'b0, instruction_register[2:0]};
            RegSelReg8Dest: reg_write_index = {1'b0, instruction_register[5:3]};
        endcase
        reg_read1_out = registers[reg_read1_index];
        reg_read2_out = registers[reg_read2_index];
        case (reg_write_input)
            RegInputAlu: reg_write_data = alu_out;
            RegInputMem: reg_write_data = mem_data_in;
        endcase
    end
    int i;
    initial begin
        for (i = 0; i < 12; i += 1) registers[i] = 0;
    end
    always_ff @(posedge clk) begin
        if (reset) begin
            for (i = 0; i < 12; i += 1) registers[i] <= 0;
        end else if (t_cycle == 3 && reg_write_enable) begin
            // TODO resolve conflict with writing to flags register.
            registers[reg_write_index] <= reg_write_data;
        end
    end

    /// Stubbed signals.
    assign mem_addr = pc;
    assign mem_data_out = 8'd0;

    `ifdef COCOTB_SIM
    initial begin
        $dumpfile ("cpu.vcd");
        $dumpvars (0, cpu);
        #1;
    end
    `endif
endmodule