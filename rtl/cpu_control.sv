typedef enum logic [1:0] {
    MicroBranchNext,
    MicroBranchJump,
    MicroBranchCond,
    MicroBranchDispatch
} microbranch_e;

typedef logic [7:0] state_t;

module cpu_control (
    /// Clock
    input clk,
    input [1:0] t_cycle,
    /// Synchronous reset
    input reset,

    /// Current memory data in.
    input [7:0] mem_data_in,
    /// Whether the current condition (in instruction register) is satisfied (based on flags)
    input condition,

    /// Control signal: how PC should be updated.
    output pc_next_e pc_next,
    /// Control signal: if the instruction reg. should be loaded with memory read.
    output logic inst_load,
    /// Control signal: the first register we're reading.
    output reg_sel_e reg_read1_sel,
    /// Control signal: the second register we're reading.
    output reg_sel_e reg_read2_sel,
    /// Control signal: the register we're (maybe) writing to.
    output reg_sel_e reg_write_sel,
    /// Control signal: register write operation to perform.
    output reg_op_e reg_op,
    /// Control signal: incrementer/decrementer operation to perform.
    output inc_op_e inc_op,
    /// Control signal: incrementer/decrementer register target.
    output inc_reg_e inc_reg,
    /// Control signal: ALU operation.
    output alu_op_e alu_op,
    /// Control signal: ALU select A.
    output alu_sel_a_e alu_sel_a,
    /// Control signal: ALU select B.
    output alu_sel_b_e alu_sel_b,
    /// Control signal: whether ALU should update flags.
    output logic alu_write_flags,
    /// Control signal: whether we're accessing memory.
    output logic mem_enable,
    /// Control signal: whether we're writing to memory (if `mem_enable`). 
    output logic mem_write,
    /// Control signal: where the memory address comes from.
    output mem_addr_sel_e mem_addr_sel
);
    state_t state = 0; // Initial state = NOP

    // Describe control given the state.
    microbranch_e microbranch;
    state_t next_state;
    always_comb begin
        // TODO: make these don't cares?
        pc_next = PcNextSame;
        inst_load = 0;
        reg_read1_sel = RegSelA;
        reg_read2_sel = RegSelA;
        reg_write_sel = RegSelA;
        reg_op = RegOpNone;
        alu_op = AluOpCopyA;
        alu_sel_a = AluSelARegA;
        alu_sel_b = AluSelBReg2;
        alu_write_flags = 0;
        mem_enable = 0;
        mem_write = 0;
        mem_addr_sel = MemAddrSelPc;
        microbranch = MicroBranchNext;
        next_state = 0;
        
        case (state)
            `include "cpu_control_signals.inc"
        endcase
    end

    // Describe next state given current state.
    always_ff @(posedge clk) begin
        if (reset) state <= 0;
        else if (t_cycle == 3) begin
            if (microbranch == MicroBranchNext) state <= state + 1;
            else if (microbranch == MicroBranchDispatch) begin
                // Dispatch based off the next memory we're reading.
                casez (mem_data_in)
                    `include "cpu_control_dispatch.inc"
                    default: state <= 1; // "INVALID" state.
                endcase
            end else if (microbranch == MicroBranchJump) state <= next_state;
            else if (microbranch == MicroBranchCond) state <= (condition ? next_state : (state + 1));
        end
    end


endmodule