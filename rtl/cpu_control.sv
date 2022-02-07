typedef enum logic [1:0] {
    MicroBranchNext,
    MicroBranchJump,
    MicroBranchCond,
    MicroBranchDispatch
} microbranch_e;

typedef logic [2:0] state_t;

module cpu_control (
    /// Clock
    input clk,
    input m_cycle,
    /// Synchronous reset
    input reset,

    /// Current instruction register.
    input [7:0] instruction_register,

    /// Control signal: how PC should be updated.
    output pc_next_e pc_next,
    /// Control signal: if the instruction reg. should be loaded with memory read.
    output logic inst_load,
    /// Control signal: whether we're accessing memory.
    output logic mem_enable,
    /// Control signal: whether we're writing to memory (if `mem_enable`).
    output logic mem_write
);
    state_t state = 0; // Initial state = NOP

    // Describe control given the state.
    microbranch_e microbranch;
    state_t next_state;
    always_comb begin
        // TODO: make these don't cares?
        pc_next = PcNextSame;
        inst_load = 0;
        mem_enable = 0;
        mem_write = 0;
        microbranch = MicroBranchNext;
        next_state = 0;
        
        case (state)
            `include "cpu_control_signals.inc"
        endcase
    end

    // Describe next state given current state.
    always_ff @(posedge clk) begin
        if (reset) state <= 0;
        else if (m_cycle) begin
            if (microbranch == MicroBranchNext) state <= state + 1;
            else if (microbranch == MicroBranchDispatch) begin
                casez (instruction_register)
                    `include "cpu_control_dispatch.inc"
                endcase
            end else if (microbranch == MicroBranchJump) state <= next_state;
            else if (microbranch == MicroBranchCond) begin
                // TODO: check condition
                state <= next_state;
            end
        end
    end


endmodule