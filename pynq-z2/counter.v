module counter (
    input wire clk_125mhz,
    input [1:0] switches,
    output [3:0] leds
);

  reg [27:0] count = 0;
  always @(posedge clk_125mhz) begin
    if (switches[0]) begin
        if (switches[1]) begin
            count <= count + 1;
        end else begin
            count <= count + 2;
        end
    end
  end

  assign leds[3:0] = count[27:24];

endmodule
