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
    wire [2:0]S_AXI_0_arprot;
    wire S_AXI_0_arready;
    wire S_AXI_0_arvalid;
    wire [31:0]S_AXI_0_awaddr;
    wire [2:0]S_AXI_0_awprot;
    wire S_AXI_0_awready;
    wire S_AXI_0_awvalid;
    wire S_AXI_0_bready;
    wire [1:0]S_AXI_0_bresp;
    wire S_AXI_0_bvalid;
    wire [31:0]S_AXI_0_rdata;
    wire S_AXI_0_rready;
    wire [1:0]S_AXI_0_rresp;
    wire S_AXI_0_rvalid;
    wire [31:0]S_AXI_0_wdata;
    wire S_AXI_0_wready;
    wire [3:0]S_AXI_0_wstrb;
    wire S_AXI_0_wvalid;

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
        .S_AXI_0_arprot(S_AXI_0_arprot),
        .S_AXI_0_arready(S_AXI_0_arready),
        .S_AXI_0_arvalid(S_AXI_0_arvalid),
        .S_AXI_0_awaddr(S_AXI_0_awaddr),
        .S_AXI_0_awprot(S_AXI_0_awprot),
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
        .S_AXI_0_wstrb(S_AXI_0_wstrb),
        .S_AXI_0_wvalid(S_AXI_0_wvalid)
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
