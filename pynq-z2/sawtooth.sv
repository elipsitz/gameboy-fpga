module sawtooth
#(
    parameter int BIT_WIDTH = 16,
    parameter int SAMPLE_RATE = 48000,
    parameter int WAVE_RATE = 480
)
(
    input logic clk_audio,
    output logic signed [BIT_WIDTH-1:0] level = BIT_WIDTH'(0)
);

localparam int NUM_PCM_STEPS = (BIT_WIDTH + 1)'(2)**(BIT_WIDTH + 1)'(BIT_WIDTH) - 1;
localparam real FREQUENCY_RATIO = real'(WAVE_RATE) / real'(SAMPLE_RATE);
localparam bit [BIT_WIDTH-1:0] INCREMENT = BIT_WIDTH'(NUM_PCM_STEPS * FREQUENCY_RATIO);

always @(posedge clk_audio)
    level <= level + INCREMENT;
endmodule
