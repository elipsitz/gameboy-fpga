/// Control signal: how PC should be updated on an M-cycle.
typedef enum logic [0:0] {
    /// Do not change PC.
    PcUpdateNone,
    /// Increment PC by 1.
    PcUpdateInc
} pc_update_e;

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
    logic [1:0] t_counter = 0;
    always_ff @(posedge clk) begin
        if (reset) t_counter <= 0;
        else t_counter <= t_counter + 1;
    end
    wire m_cycle = (t_counter == 3);

    //////////////////////////////////////// Control Unit
    wire pc_update_e pc_update;
    wire load_instruction_register;
    logic [7:0] instruction_register; // Holds the current instruction.
    cpu_control control (
        .clk,
        .reset,
        .instruction_register,
        .pc_update,
        .load_instruction_register,
        .mem_enable,
        .mem_write
    );
    always_ff @(posedge clk) begin
        // Handle instruction register.
        if (reset) instruction_register <= 0;
        else if (load_instruction_register) instruction_register <= mem_data_in;
    end

    //////////////////////////////////////// Program Counter
    logic [15:0] pc = 0; // Current PC
    always_ff @(posedge clk) begin
        if (reset) pc <= 0;
        else if (m_cycle) begin
            if (pc_update == PcUpdateInc) pc <= pc + 16'd1;
        end
    end

    /// Stubbed signals.
    assign mem_addr = pc;
    assign mem_data_out = 8'd0;
endmodule