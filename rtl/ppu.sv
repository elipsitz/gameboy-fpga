`timescale 1ns/1ns

/// PPU
module ppu (
    /// Clock (4 MHz)
    input clk,
    /// Synchronous reset
    input reset,

    /// Output pixel value.
    output logic [1:0] pixel_out,
    /// Whether the pixel is valid.
    output logic       pixel_valid
);
    // Dummy pixel output.
    assign pixel_valid = 1;
    always_ff @ (posedge clk) begin
        if (reset) begin
            pixel_out <= 0;
        end else begin
            pixel_out <= pixel_out + 1;
        end
    end
endmodule
