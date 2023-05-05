module top_pynq_z2 (
    // The input clock
    input clk_125mhz,

    // HDMI pins
    output hdmi_out_clk_n,
    output hdmi_out_clk_p,
    output [2:0] hdmi_out_data_n,
    output [2:0] hdmi_out_data_p,
    input [0:0] hdmi_out_hpd,

    // Connections to cartridge
    output [15:0] cartridge_A,
    inout [7:0] cartridge_D,
    output cartridge_nWR,
    output cartridge_nRD,
    output cartridge_nCS,
    output cartridge_nRST,

    // Switches, buttons, and LEDs on the board
    input [1:0] switches,
    input [3:0] buttons,
    output [3:0] leds
);
    logic clock_reset = 1'd0;
    logic clk_pixel;
    logic clk_pixel_x5;
    logic clk_8mhz;
    clk_wiz_0 clk_wiz_0(
        .clk_pixel(clk_pixel),
        .clk_pixel_x5(clk_pixel_x5),
        .clk_8mhz(clk_8mhz),
        .reset(clock_reset),
        .clk_in1(clk_125mhz)
    );

    /////////////////////////////////////////////////
    // Audio clock
    /////////////////////////////////////////////////
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

    /////////////////////////////////////////////////
    // 4.194 MHz clock for Gameboy
    /////////////////////////////////////////////////
    logic clk_4mhz;
    logic [0:0] counter_4mhz;
    always_ff @(posedge clk_8mhz) begin 
        counter_4mhz <= counter_4mhz + 1;
    end
    assign clk_4mhz = counter_4mhz[0];
    /////////

    /////////////////////////////////////////////////
    // Reset
    /////////////////////////////////////////////////
    logic reset;
    logic reset_pre1;
    logic reset_pre2;
    // TODO: different reset for each clock?
    always_ff @(posedge clk_4mhz) begin
        reset_pre2 <= switches[1];
        reset_pre1 <= reset_pre2;
        reset <= reset_pre1;
    end

    /////////////////////////////////////////////////
    // Framebuffer
    /////////////////////////////////////////////////
    parameter RAM_WIDTH = 2;                  // Specify RAM data width
    parameter RAM_DEPTH = 256 * 144;                  // Specify RAM depth (number of entries)
    parameter RAM_PERFORMANCE = "HIGH_PERFORMANCE"; // Select "HIGH_PERFORMANCE" or "LOW_LATENCY" 

    logic [clogb2(RAM_DEPTH-1)-1:0] framebuffer_write_addr; // Write address bus, width determined from RAM_DEPTH
    logic [clogb2(RAM_DEPTH-1)-1:0] framebuffer_read_addr; // Read address bus, width determined from RAM_DEPTH
    logic [RAM_WIDTH-1:0] framebuffer_write_data;          // RAM input data
    logic framebuffer_write_en;                           // Write enable
    logic framebuffer_read_en;                           // Read Enable, for additional power savings, disable when not in use
    logic framebuffer_output_en;                        // Output register enable
    wire [RAM_WIDTH-1:0] framebuffer_read_data;                  // RAM output data

    reg [RAM_WIDTH-1:0] framebuffer [RAM_DEPTH-1:0];
    reg [RAM_WIDTH-1:0] framebuffer_output = {RAM_WIDTH{1'b0}};

    // Write
    always @(posedge clk_4mhz)
      if (framebuffer_write_en)
        framebuffer[framebuffer_write_addr] <= framebuffer_write_data;

    // Read
    always @(posedge clk_pixel)
      if (framebuffer_read_en)
        framebuffer_output <= framebuffer[framebuffer_read_addr];

        reg [RAM_WIDTH-1:0] doutb_reg = {RAM_WIDTH{1'b0}};

        always @(posedge clk_pixel)
          if (reset)
            doutb_reg <= {RAM_WIDTH{1'b0}};
          else if (framebuffer_output_en)
            doutb_reg <= framebuffer_output;

        assign framebuffer_read_data = doutb_reg;

    // The following function calculates the address width based on specified RAM depth
    function integer clogb2;
      input integer depth;
        for (clogb2=0; depth>0; clogb2=clogb2+1)
          depth = depth >> 1;
    endfunction
                        

    /////////////////////////////////////////////////
    // Gameboy
    /////////////////////////////////////////////////
    logic [7:0] gb_dataRead;
    logic [7:0] gb_dataWrite;
    logic [15:0] gb_cart_address;
    logic gb_cart_readEnable;
    logic gb_cart_writeEnable;
    logic gb_cart_chipSelect;

    // Hacky temporary joypad
    logic joypad_start = buttons[0] && switches[0];
    logic joypad_select = buttons[1] && switches[0];
    logic joypad_b = buttons[2] && switches[0];
    logic joypad_a = buttons[3] && switches[0];
    logic joypad_down = buttons[0] && !switches[0];
    logic joypad_up = buttons[1] && !switches[0];
    logic joypad_left = buttons[2] && !switches[0];
    logic joypad_right = buttons[3] && !switches[0];

    logic [9:0] gb_audio_left;
    logic [9:0] gb_audio_right;

    logic [1:0] gb_pixel;
    logic gb_ppu_vblank;
    logic gb_ppu_hblank;
    logic gb_ppu_lcdEnable;
    logic gb_ppu_valid;

    Gameboy Gameboy(
        .clock(clk_4mhz),
        .reset(reset),
        .io_cartridge_dataRead(gb_dataRead),
        .io_cartridge_dataWrite(gb_dataWrite),
        .io_cartridge_address(gb_cart_address),
        .io_cartridge_readEnable(gb_cart_readEnable),
        .io_cartridge_writeEnable(gb_cart_writeEnable),
        .io_cartridge_chipSelect(gb_cart_chipSelect),
        .io_joypad_start(joypad_start),
        .io_joypad_select(joypad_select),
        .io_joypad_b(joypad_b),
        .io_joypad_a(joypad_a),
        .io_joypad_down(joypad_down),
        .io_joypad_up(joypad_up),
        .io_joypad_left(joypad_left),
        .io_joypad_right(joypad_right),
        .io_apu_left(gb_audio_left),
        .io_apu_right(gb_audio_right),
        .io_ppu_pixel(gb_pixel),
        .io_ppu_vblank(gb_ppu_vblank),
        .io_ppu_hblank(gb_ppu_hblank),
        .io_ppu_lcdEnable(gb_ppu_lcdEnable),
        .io_ppu_valid(gb_ppu_valid)
    );

    /////////////////////////////////////////////////
    // Physical Cartridge I/O
    /////////////////////////////////////////////////
    assign gb_dataRead = cartridge_D;
    assign cartridge_D = gb_cart_writeEnable ? gb_dataWrite : 8'hzz;
    assign cartridge_A = gb_cart_address;
    assign cartridge_nWR = ~gb_cart_writeEnable;
    assign cartridge_nRD = ~gb_cart_readEnable;
    assign cartridge_nCS = gb_cart_chipSelect; // high for ROM low for RAM 
    assign cartridge_nRST = ~reset;

    /////////////////////////////////////////////////
    // Gameboy PPU output
    /////////////////////////////////////////////////
    logic [7:0] fb_x = 0;
    logic [7:0] fb_y = 0;
    logic prev_hblank;

    always @(posedge clk_4mhz) begin
        prev_hblank <= gb_ppu_hblank;

        if (gb_ppu_vblank) begin
            fb_y <= 0;
            fb_x <= 0;
        end else if (gb_ppu_hblank && !prev_hblank) begin
            fb_x <= 0;
            fb_y <= fb_y + 1;
        end

        if (gb_ppu_valid) begin
            framebuffer_write_en <= 1;
            framebuffer_write_addr <= {fb_y, fb_x};
            framebuffer_write_data <= gb_pixel;
            fb_x <= fb_x + 1;
        end else begin
            framebuffer_write_en <= 0;
        end
    end

    /////////////////////////////////////////////////
    // Gameboy Audio output
    /////////////////////////////////////////////////
    logic [15:0] audio_sample_word_buffer [1:0];
    logic [15:0] audio_sample_word [1:0];
    always @(posedge clk_audio) begin
        audio_sample_word_buffer[0] <= {gb_audio_left, 6'd0};
        audio_sample_word_buffer[1] <= {gb_audio_right, 6'd0};
        audio_sample_word <= audio_sample_word_buffer;
    end

    /////////////////////////////////////////////////
    // Debug LEDs and Test Points
    /////////////////////////////////////////////////
    assign leds[0] = gb_audio_left[9];
    assign leds[1] = gb_ppu_vblank;
    assign leds[2] = !gb_ppu_lcdEnable;
    assign leds[3] = gb_cart_readEnable || gb_cart_writeEnable;

    /////////////////////////////////////////////////
    // HDMI picture generation
    /////////////////////////////////////////////////
    logic [23:0] rgb = 24'd0;
    logic [9:0] cx, cy, screen_start_x, screen_start_y, frame_width, frame_height, screen_width, screen_height;
    always @(posedge clk_pixel) begin
        // This is delayed by 2 cycles, so it's off but it should still show something
        framebuffer_read_addr <= {cy[7:0], cx[7:0]};
        framebuffer_read_en <= 1;
        framebuffer_output_en <= 1;
        rgb <= {12{~framebuffer_read_data}};
    end

    /////////////////////////////////////////////////
    // HDMI Output: 640x480 @ 60.00Hz
    /////////////////////////////////////////////////
    logic [2:0] tmds;
    logic tmds_clock;
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
