module top_pynq_overlay_test (
    // Switches, buttons, and LEDs on the board
    input [1:0] switches,
    input [3:0] buttons,
    output [3:0] leds
);
    logic clk;
    logic [63:0] gpio_i;
    logic [63:0] gpio_o;
    logic [63:0] gpio_t;

    zynq_ps_wrapper zynq_ps_wrapper(
        .FCLK_CLK0(clk),
        .GPIO_I(gpio_i),
        .GPIO_O(gpio_o),
        .GPIO_T(gpio_t)
    );


    assign leds = gpio_o[3:0];
    assign gpio_i[6:3] = buttons[3:0];
    assign gpio_i[8:7] = switches[1:0];
endmodule
