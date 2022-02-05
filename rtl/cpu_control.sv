module cpu_control (
    /// Clock
    input clk,
    /// Synchronous reset
    input reset,

    /// Current instruction register.
    input [7:0] instruction_register,

    /// Control signal: how PC should be updated.
    output pc_update_e pc_update,
    /// Control signal: if the instruction reg. should be loaded with memory read.
    output load_instruction_register,
    /// Control signal: whether we're accessing memory.
    output mem_enable,
    /// Control signal: whether we're writing to memory (if `mem_enable`).
    output mem_write
);

    assign pc_update = PcUpdateInc;
    assign load_instruction_register = 1;
    assign mem_enable = 1;
    assign mem_write = 0;
endmodule