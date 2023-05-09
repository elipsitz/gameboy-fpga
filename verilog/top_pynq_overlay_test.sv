module top_pynq_overlay_test (
    // Switches, buttons, and LEDs on the board
    input [1:0] switches,
    input [3:0] buttons,
    output [3:0] leds
);
    /////////////////////////////////////////////////
    // Zynq PS
    /////////////////////////////////////////////////
    logic clk;
    logic [0:0]peripheral_reset;

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

    logic reset = peripheral_reset[0];
    logic resetn = !reset;
    ZynqGameboy zynq_gameboy(
        .clock(clk),
        .reset(reset),
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
        .io_axiInitiator_bresp(S_AXI_0_bresp),

        .io_leds(leds)
    );
endmodule
