module top_pynq_z2 (
    input clk_125mhz,

    output hdmi_out_clk_n,
    output hdmi_out_clk_p,
    output [2:0] hdmi_out_data_n,
    output [2:0] hdmi_out_data_p,
    input [0:0] hdmi_out_hpd,

    input [1:0] switches,
    input [3:0] buttons,
    output [3:0] leds
);
    logic reset;
    logic clk_pixel;
    logic clk_pixel_x5;
    logic clk_audio;

    clk_wiz_0 clk_wiz_0(
        .clk_hdmi1(clk_pixel),
        .clk_hdmi2(clk_pixel_x5),
        .reset(reset),
        .clk_in1(clk_125mhz)
    );

    assign reset = buttons[0];
    assign leds[0] = 1'd1;
    assign leds[1] = !hdmi_out_hpd; // active low

    localparam AUDIO_BIT_WIDTH = 16;
    localparam AUDIO_RATE = 48000;
    localparam WAVE_RATE = 480;

    logic clk_audio;
    logic [8:0] audio_divider;

    always_ff @(posedge clk_pixel) 
    begin
        // Dividing clk_pixel -- (25.2MHz / 262) = 96.183kHz (2x 48Khz)
        if (audio_divider < 9'd262-1) audio_divider <= audio_divider + 1;
        else begin
            clk_audio <= ~clk_audio;
            audio_divider <= 0;
        end
    end

    logic [15:0] audio_increment;
    always @(*) begin
        if (!buttons[2] && !buttons[3]) audio_increment = 16'd600;
        else if (buttons[2] && !buttons[3]) audio_increment = 16'd1200;
        else if (!buttons[2] && buttons[3]) audio_increment = 16'd300;
        else audio_increment = 16'd150;
    end

    logic [15:0] audio_sample_word [1:0];// = '{16'd0, 16'd0};
    always_ff @(posedge clk_audio) begin
        if (switches[0]) begin
            audio_sample_word[0] <= audio_sample_word[0] + audio_increment;
            audio_sample_word[1] <= audio_sample_word[1] + audio_increment;
        end
    end 


    logic [23:0] rgb = 24'd0;
    logic [9:0] cx, cy, screen_start_x, screen_start_y, frame_width, frame_height, screen_width, screen_height;
    // Border test (left = red, top = green, right = blue, bottom = blue, fill = black)
    always @(posedge clk_pixel)
    //  rgb <= {cx == 0 ? ~8'd0 : 8'd0, cy == 0 ? ~8'd0 : 8'd0, cx == screen_width - 1'd1 || cy == screen_width - 1'd1 ? ~8'd0 : 8'd0};
        rgb <= {buttons[1] ? (8'(cx) ^ 8'(cy)) : 8'd0, 8'(cx), 8'(cy)};

    logic [2:0] tmds;
    logic tmds_clock;

    // 640x480 @ 60.00Hz
    hdmi #(.VIDEO_ID_CODE(1), .VIDEO_REFRESH_RATE(60.00), .AUDIO_RATE(AUDIO_RATE), .AUDIO_BIT_WIDTH(AUDIO_BIT_WIDTH)) hdmi(
      .clk_pixel_x5(clk_pixel_x5),
      .clk_pixel(clk_pixel),
      .clk_audio(clk_audio),
      .reset(reset),
      .rgb(rgb),
      .audio_sample_word(audio_sample_word),
      .tmds(tmds),
      .tmds_clock(tmds_clock),
      .cx(cx),
      .cy(cy),
      .frame_width(frame_width),
      .frame_height(frame_height),
      .screen_width(screen_width),
      .screen_height(screen_height)
    );

    OBUFDS #(.IOSTANDARD("TMDS_33")) obufds0 (.I(tmds[0]), .O(hdmi_out_data_p[0]), .OB(hdmi_out_data_n[0]));
    OBUFDS #(.IOSTANDARD("TMDS_33")) obufds1 (.I(tmds[1]), .O(hdmi_out_data_p[1]), .OB(hdmi_out_data_n[1]));
    OBUFDS #(.IOSTANDARD("TMDS_33")) obufds2 (.I(tmds[2]), .O(hdmi_out_data_p[2]), .OB(hdmi_out_data_n[2]));
    OBUFDS #(.IOSTANDARD("TMDS_33")) obufds_clock(.I(tmds_clock), .O(hdmi_out_clk_p), .OB(hdmi_out_clk_n));

endmodule
