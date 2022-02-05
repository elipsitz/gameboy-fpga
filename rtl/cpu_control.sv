module cpu_control (
    /// Clock
    input clk,
    /// Synchronous reset
    input reset,

    /// Current instruction register.
    input [7:0] reg_instruction,

    /// Control signal: how PC should be updated.
    output pc_update_e pc_update
);

    assign pc_update = PcUpdateInc;
endmodule