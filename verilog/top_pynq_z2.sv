module top_pynq_z2 (
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
    /////////////////////////////////////////////////
    // Zynq PS
    /////////////////////////////////////////////////
    logic clk;
    logic clk_8mhz;
    logic clk_pixel;
    logic clk_pixel_x5;
    logic clk_gameboy;
    logic [0:0]peripheral_reset;
    logic [63:0]GPIO_I;
    logic [63:0]GPIO_O;
    logic [63:0]GPIO_T;

    wire [31:0]M_AXI_0_araddr;
    wire M_AXI_0_arready;
    wire M_AXI_0_arvalid;
    wire [31:0]M_AXI_0_awaddr;
    wire M_AXI_0_awready;
    wire M_AXI_0_awvalid;
    wire M_AXI_0_bready;
    wire [1:0]M_AXI_0_bresp;
    wire M_AXI_0_bvalid;
    wire [31:0]M_AXI_0_rdata;
    wire M_AXI_0_rready;
    wire [1:0]M_AXI_0_rresp;
    wire M_AXI_0_rvalid;
    wire [31:0]M_AXI_0_wdata;
    wire M_AXI_0_wready;
    wire M_AXI_0_wvalid;

    wire [31:0]S_AXI_0_araddr;
    wire S_AXI_0_arready;
    wire S_AXI_0_arvalid;
    wire [31:0]S_AXI_0_awaddr;
    wire S_AXI_0_awready;
    wire S_AXI_0_awvalid;
    wire S_AXI_0_bready;
    wire [1:0]S_AXI_0_bresp;
    wire S_AXI_0_bvalid;
    wire [63:0]S_AXI_0_rdata;
    wire S_AXI_0_rready;
    wire [1:0]S_AXI_0_rresp;
    wire S_AXI_0_rvalid;
    wire [63:0]S_AXI_0_wdata;
    wire S_AXI_0_wready;
    wire S_AXI_0_wvalid;

    // AXI3 signals not used in AXI4-lite
    wire [1:0]S_AXI_0_arburst = 2'b01;
    wire [3:0]S_AXI_0_arcache = 4'b0000;
    wire [5:0]S_AXI_0_arid = 6'd0;
    wire [3:0]S_AXI_0_arlen = 4'd0;
    wire [1:0]S_AXI_0_arlock = 1'b0;
    wire [2:0]S_AXI_0_arprot = 3'b000; // Used in AXI-Lite, but unused by this implementation.
    wire [3:0]S_AXI_0_arqos = 4'd0;
    wire [2:0]S_AXI_0_arsize = 3'b011; // (or 3'b010 for 32-bit)
    wire [1:0]S_AXI_0_awburst = 2'b01;
    wire [3:0]S_AXI_0_awcache = 4'b0000;
    wire [5:0]S_AXI_0_awid = 6'd0;
    wire [3:0]S_AXI_0_awlen = 4'd0;
    wire [1:0]S_AXI_0_awlock = 1'b0;
    wire [2:0]S_AXI_0_awprot = 3'b000; // Used in AXI-Lite, but unused by this implementation.
    wire [3:0]S_AXI_0_awqos = 4'd0;
    wire [2:0]S_AXI_0_awsize = 3'b011; // (or 3'b010 for 32-bit)
    wire [5:0]S_AXI_0_bid; // Output, unused.
    wire [5:0]S_AXI_0_rid; // Output, unused.
    wire S_AXI_0_rlast;  // Output, unused.
    wire [5:0]S_AXI_0_wid = 6'd0;
    wire S_AXI_0_wlast = 1'b1;
    wire [7:0]S_AXI_0_wstrb = 8'b11111111; // TODO: will need to use this for writes.

    zynq_ps zynq_ps_i(
        .peripheral_reset(peripheral_reset),
        .FCLK_CLK0(clk),
        .clk_8mhz(clk_8mhz),
        .clk_pixel(clk_pixel),
        .clk_pixel_x5(clk_pixel_x5),
        .GPIO_I(GPIO_I),
        .GPIO_O(GPIO_O),
        .GPIO_T(GPIO_T),

        .M_AXI_0_araddr(M_AXI_0_araddr),
        .M_AXI_0_arready(M_AXI_0_arready),
        .M_AXI_0_arvalid(M_AXI_0_arvalid),
        .M_AXI_0_awaddr(M_AXI_0_awaddr),
        .M_AXI_0_awready(M_AXI_0_awready),
        .M_AXI_0_awvalid(M_AXI_0_awvalid),
        .M_AXI_0_bready(M_AXI_0_bready),
        .M_AXI_0_bresp(M_AXI_0_bresp),
        .M_AXI_0_bvalid(M_AXI_0_bvalid),
        .M_AXI_0_rdata(M_AXI_0_rdata),
        .M_AXI_0_rready(M_AXI_0_rready),
        .M_AXI_0_rresp(M_AXI_0_rresp),
        .M_AXI_0_rvalid(M_AXI_0_rvalid),
        .M_AXI_0_wdata(M_AXI_0_wdata),
        .M_AXI_0_wready(M_AXI_0_wready),
        .M_AXI_0_wvalid(M_AXI_0_wvalid),

        .S_AXI_0_araddr(S_AXI_0_araddr),
        .S_AXI_0_arready(S_AXI_0_arready),
        .S_AXI_0_arvalid(S_AXI_0_arvalid),
        .S_AXI_0_awaddr(S_AXI_0_awaddr),
        .S_AXI_0_awready(S_AXI_0_awready),
        .S_AXI_0_awvalid(S_AXI_0_awvalid),
        .S_AXI_0_bready(S_AXI_0_bready),
        .S_AXI_0_bresp(S_AXI_0_bresp),
        .S_AXI_0_bvalid(S_AXI_0_bvalid),
        .S_AXI_0_rdata(S_AXI_0_rdata),
        .S_AXI_0_rready(S_AXI_0_rready),
        .S_AXI_0_rresp(S_AXI_0_rresp),
        .S_AXI_0_rvalid(S_AXI_0_rvalid),
        .S_AXI_0_wdata(S_AXI_0_wdata),
        .S_AXI_0_wready(S_AXI_0_wready),
        .S_AXI_0_wvalid(S_AXI_0_wvalid),

        .S_AXI_0_arburst(S_AXI_0_arburst),
        .S_AXI_0_arcache(S_AXI_0_arcache),
        .S_AXI_0_arid(S_AXI_0_arid),
        .S_AXI_0_arlen(S_AXI_0_arlen),
        .S_AXI_0_arlock(S_AXI_0_arlock),
        .S_AXI_0_arprot(S_AXI_0_arprot),
        .S_AXI_0_arqos(S_AXI_0_arqos),
        .S_AXI_0_arsize(S_AXI_0_arsize),
        .S_AXI_0_awburst(S_AXI_0_awburst),
        .S_AXI_0_awcache(S_AXI_0_awcache),
        .S_AXI_0_awid(S_AXI_0_awid),
        .S_AXI_0_awlen(S_AXI_0_awlen),
        .S_AXI_0_awlock(S_AXI_0_awlock),
        .S_AXI_0_awprot(S_AXI_0_awprot),
        .S_AXI_0_awqos(S_AXI_0_awqos),
        .S_AXI_0_awsize(S_AXI_0_awsize),
        .S_AXI_0_bid(S_AXI_0_bid),
        .S_AXI_0_rid(S_AXI_0_rid),
        .S_AXI_0_rlast(S_AXI_0_rlast),
        .S_AXI_0_wid(S_AXI_0_wid),
        .S_AXI_0_wlast(S_AXI_0_wlast),
        .S_AXI_0_wstrb(S_AXI_0_wstrb)
    );

    /////////////////////////////////////////////////
    // Audio clock
    /////////////////////////////////////////////////
    localparam AUDIO_BIT_WIDTH = 16;
    localparam AUDIO_RATE = 48000;

    logic clk_audio;
    logic [8:0] audio_divider;
    always_ff @(posedge clk_pixel) 
    begin
        // Dividing clk_pixel -- (25.2MHz / 262) = 96.183kHz (2x 48Khz)
        if (audio_divider < 9'd262 - 1) audio_divider <= audio_divider + 1;
        else begin
            clk_audio <= ~clk_audio;
            audio_divider <= 0;
        end
    end

    /////////////////////////////////////////////////
    // Reset
    /////////////////////////////////////////////////
    logic reset;
    logic reset_pre1;
    logic reset_pre2;
    // TODO: different reset for each clock?
    always_ff @(posedge clk) begin
        reset_pre2 <= buttons[0] || peripheral_reset[0];
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
    always @(posedge clk_gameboy)
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
    logic [1:0] gb_tCycle;
    logic [7:0] gb_dataRead;
    logic [7:0] gb_dataWrite;
    logic [15:0] gb_cart_address;
    logic gb_cart_enable;
    logic gb_cart_write;
    logic gb_cart_chipSelect;

    logic joypad_start = GPIO_O[8];
    logic joypad_select = GPIO_O[9];
    logic joypad_b = GPIO_O[10];
    logic joypad_a = GPIO_O[11];
    logic joypad_down = GPIO_O[12];
    logic joypad_up = GPIO_O[13];
    logic joypad_left = GPIO_O[14];
    logic joypad_right = GPIO_O[15];

    logic [9:0] gb_audio_left;
    logic [9:0] gb_audio_right;

    logic [1:0] gb_pixel;
    logic gb_ppu_vblank;
    logic gb_ppu_hblank;
    logic gb_ppu_lcdEnable;
    logic gb_ppu_valid;

    ZynqGameboy zynq_gameboy(
        .clock(clk),
        .reset(reset),
        .io_clock_8mhz(clk_8mhz),
        .io_clock_gameboy(clk_gameboy),
        .io_cartridge_dataRead(gb_dataRead),
        .io_cartridge_dataWrite(gb_dataWrite),
        .io_cartridge_address(gb_cart_address),
        .io_cartridge_enable(gb_cart_enable),
        .io_cartridge_write(gb_cart_write),
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
        .io_ppu_valid(gb_ppu_valid),
        .io_tCycle(gb_tCycle),

        .io_axiTarget_arvalid(M_AXI_0_arvalid),
        .io_axiTarget_arready(M_AXI_0_arready),
        .io_axiTarget_araddr(M_AXI_0_araddr),
        .io_axiTarget_rvalid(M_AXI_0_rvalid),
        .io_axiTarget_rready(M_AXI_0_rready),
        .io_axiTarget_rdata(M_AXI_0_rdata),
        .io_axiTarget_rresp(M_AXI_0_rresp),
        .io_axiTarget_awvalid(M_AXI_0_awvalid),
        .io_axiTarget_awready(M_AXI_0_awready),
        .io_axiTarget_awaddr(M_AXI_0_awaddr),
        .io_axiTarget_wvalid(M_AXI_0_wvalid),
        .io_axiTarget_wready(M_AXI_0_wready),
        .io_axiTarget_wdata(M_AXI_0_wdata),
        .io_axiTarget_bvalid(M_AXI_0_bvalid),
        .io_axiTarget_bready(M_AXI_0_bready),
        .io_axiTarget_bresp(M_AXI_0_bresp),

        .io_axiInitiator_arvalid(S_AXI_0_arvalid),
        .io_axiInitiator_arready(S_AXI_0_arready),
        .io_axiInitiator_araddr(S_AXI_0_araddr),
        .io_axiInitiator_rvalid(S_AXI_0_rvalid),
        .io_axiInitiator_rready(S_AXI_0_rready),
        .io_axiInitiator_rdata(S_AXI_0_rdata),
        .io_axiInitiator_rresp(S_AXI_0_rresp),
        .io_axiInitiator_awvalid(S_AXI_0_awvalid),
        .io_axiInitiator_awready(S_AXI_0_awready),
        .io_axiInitiator_awaddr(S_AXI_0_awaddr),
        .io_axiInitiator_wvalid(S_AXI_0_wvalid),
        .io_axiInitiator_wready(S_AXI_0_wready),
        .io_axiInitiator_wdata(S_AXI_0_wdata),
        .io_axiInitiator_bvalid(S_AXI_0_bvalid),
        .io_axiInitiator_bready(S_AXI_0_bready),
        .io_axiInitiator_bresp(S_AXI_0_bresp)
    );

    /////////////////////////////////////////////////
    // Physical Cartridge I/O
    /////////////////////////////////////////////////
    assign gb_dataRead = cartridge_D;
    assign cartridge_D = (gb_cart_enable && gb_cart_write) ? gb_dataWrite : 8'hzz;
    assign cartridge_A = gb_cart_address;
    assign cartridge_nWR = ~(gb_cart_enable && gb_cart_write && (gb_tCycle == 2'd1 || gb_tCycle == 2'd2));
    assign cartridge_nRD = ~cartridge_nWR;
    assign cartridge_nCS = gb_cart_chipSelect; // high for ROM low for RAM 
    assign cartridge_nRST = ~reset;

    /////////////////////////////////////////////////
    // Gameboy PPU output
    /////////////////////////////////////////////////
    logic [7:0] fb_x = 0;
    logic [7:0] fb_y = 0;
    logic prev_hblank;

    always @(posedge clk_gameboy) begin
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
    assign leds[3] = gb_cart_enable;

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
