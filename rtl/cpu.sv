module cpu (
    /// Clock (normally 4 MHz)
    input clk,
    /// Synchronous reset signal (active high)
    input reset,

    /// System bus address selection.
    output [15:0] mem_addr,
    /// System bus read enable.
    output        mem_read_enable,
    /// System bus write enable.
    output        mem_write_enable,
    /// System bus data in.
    input  [7:0]  mem_data_in,
    /// System bus data out.
    output [7:0]  mem_data_out
);
    /// Program Counter.
    logic [15:0] pc = 0;
    always_ff @(posedge clk) begin
        pc <= pc + 16'd1;
    end

    /// Stubbed signals.
    assign mem_addr = pc;
    assign mem_read_enable = 1;
    assign mem_write_enable = 0;
    assign mem_data_out = 8'd0;
endmodule