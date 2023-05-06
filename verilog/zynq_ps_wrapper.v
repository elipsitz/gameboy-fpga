//Copyright 1986-2020 Xilinx, Inc. All Rights Reserved.
//--------------------------------------------------------------------------------
//Tool Version: Vivado v.2020.2 (lin64) Build 3064766 Wed Nov 18 09:12:47 MST 2020
//Date        : Sat May  6 01:26:59 2023
//Host        : eli-linux-vm running 64-bit Ubuntu 20.04.6 LTS
//Command     : generate_target zynq_ps_wrapper.bd
//Design      : zynq_ps_wrapper
//Purpose     : IP block netlist
//--------------------------------------------------------------------------------
`timescale 1 ps / 1 ps

module zynq_ps_wrapper
   (DDR_addr,
    DDR_ba,
    DDR_cas_n,
    DDR_ck_n,
    DDR_ck_p,
    DDR_cke,
    DDR_cs_n,
    DDR_dm,
    DDR_dq,
    DDR_dqs_n,
    DDR_dqs_p,
    DDR_odt,
    DDR_ras_n,
    DDR_reset_n,
    DDR_we_n,
    FCLK_CLK0,
    FIXED_IO_ddr_vrn,
    FIXED_IO_ddr_vrp,
    FIXED_IO_mio,
    FIXED_IO_ps_clk,
    FIXED_IO_ps_porb,
    FIXED_IO_ps_srstb,
    GPIO_I,
    GPIO_O,
    GPIO_T,
    M_AXI_GP0_araddr,
    M_AXI_GP0_arburst,
    M_AXI_GP0_arcache,
    M_AXI_GP0_arid,
    M_AXI_GP0_arlen,
    M_AXI_GP0_arlock,
    M_AXI_GP0_arprot,
    M_AXI_GP0_arqos,
    M_AXI_GP0_arready,
    M_AXI_GP0_arsize,
    M_AXI_GP0_arvalid,
    M_AXI_GP0_awaddr,
    M_AXI_GP0_awburst,
    M_AXI_GP0_awcache,
    M_AXI_GP0_awid,
    M_AXI_GP0_awlen,
    M_AXI_GP0_awlock,
    M_AXI_GP0_awprot,
    M_AXI_GP0_awqos,
    M_AXI_GP0_awready,
    M_AXI_GP0_awsize,
    M_AXI_GP0_awvalid,
    M_AXI_GP0_bid,
    M_AXI_GP0_bready,
    M_AXI_GP0_bresp,
    M_AXI_GP0_bvalid,
    M_AXI_GP0_rdata,
    M_AXI_GP0_rid,
    M_AXI_GP0_rlast,
    M_AXI_GP0_rready,
    M_AXI_GP0_rresp,
    M_AXI_GP0_rvalid,
    M_AXI_GP0_wdata,
    M_AXI_GP0_wid,
    M_AXI_GP0_wlast,
    M_AXI_GP0_wready,
    M_AXI_GP0_wstrb,
    M_AXI_GP0_wvalid,
    S_AXI_GP0_araddr,
    S_AXI_GP0_arburst,
    S_AXI_GP0_arcache,
    S_AXI_GP0_arid,
    S_AXI_GP0_arlen,
    S_AXI_GP0_arlock,
    S_AXI_GP0_arprot,
    S_AXI_GP0_arqos,
    S_AXI_GP0_arready,
    S_AXI_GP0_arsize,
    S_AXI_GP0_arvalid,
    S_AXI_GP0_awaddr,
    S_AXI_GP0_awburst,
    S_AXI_GP0_awcache,
    S_AXI_GP0_awid,
    S_AXI_GP0_awlen,
    S_AXI_GP0_awlock,
    S_AXI_GP0_awprot,
    S_AXI_GP0_awqos,
    S_AXI_GP0_awready,
    S_AXI_GP0_awsize,
    S_AXI_GP0_awvalid,
    S_AXI_GP0_bid,
    S_AXI_GP0_bready,
    S_AXI_GP0_bresp,
    S_AXI_GP0_bvalid,
    S_AXI_GP0_rdata,
    S_AXI_GP0_rid,
    S_AXI_GP0_rlast,
    S_AXI_GP0_rready,
    S_AXI_GP0_rresp,
    S_AXI_GP0_rvalid,
    S_AXI_GP0_wdata,
    S_AXI_GP0_wid,
    S_AXI_GP0_wlast,
    S_AXI_GP0_wready,
    S_AXI_GP0_wstrb,
    S_AXI_GP0_wvalid);
  inout [14:0]DDR_addr;
  inout [2:0]DDR_ba;
  inout DDR_cas_n;
  inout DDR_ck_n;
  inout DDR_ck_p;
  inout DDR_cke;
  inout DDR_cs_n;
  inout [3:0]DDR_dm;
  inout [31:0]DDR_dq;
  inout [3:0]DDR_dqs_n;
  inout [3:0]DDR_dqs_p;
  inout DDR_odt;
  inout DDR_ras_n;
  inout DDR_reset_n;
  inout DDR_we_n;
  output FCLK_CLK0;
  inout FIXED_IO_ddr_vrn;
  inout FIXED_IO_ddr_vrp;
  inout [53:0]FIXED_IO_mio;
  inout FIXED_IO_ps_clk;
  inout FIXED_IO_ps_porb;
  inout FIXED_IO_ps_srstb;
  input [63:0]GPIO_I;
  output [63:0]GPIO_O;
  output [63:0]GPIO_T;
  output [31:0]M_AXI_GP0_araddr;
  output [1:0]M_AXI_GP0_arburst;
  output [3:0]M_AXI_GP0_arcache;
  output [11:0]M_AXI_GP0_arid;
  output [3:0]M_AXI_GP0_arlen;
  output [1:0]M_AXI_GP0_arlock;
  output [2:0]M_AXI_GP0_arprot;
  output [3:0]M_AXI_GP0_arqos;
  input M_AXI_GP0_arready;
  output [2:0]M_AXI_GP0_arsize;
  output M_AXI_GP0_arvalid;
  output [31:0]M_AXI_GP0_awaddr;
  output [1:0]M_AXI_GP0_awburst;
  output [3:0]M_AXI_GP0_awcache;
  output [11:0]M_AXI_GP0_awid;
  output [3:0]M_AXI_GP0_awlen;
  output [1:0]M_AXI_GP0_awlock;
  output [2:0]M_AXI_GP0_awprot;
  output [3:0]M_AXI_GP0_awqos;
  input M_AXI_GP0_awready;
  output [2:0]M_AXI_GP0_awsize;
  output M_AXI_GP0_awvalid;
  input [11:0]M_AXI_GP0_bid;
  output M_AXI_GP0_bready;
  input [1:0]M_AXI_GP0_bresp;
  input M_AXI_GP0_bvalid;
  input [31:0]M_AXI_GP0_rdata;
  input [11:0]M_AXI_GP0_rid;
  input M_AXI_GP0_rlast;
  output M_AXI_GP0_rready;
  input [1:0]M_AXI_GP0_rresp;
  input M_AXI_GP0_rvalid;
  output [31:0]M_AXI_GP0_wdata;
  output [11:0]M_AXI_GP0_wid;
  output M_AXI_GP0_wlast;
  input M_AXI_GP0_wready;
  output [3:0]M_AXI_GP0_wstrb;
  output M_AXI_GP0_wvalid;
  input [31:0]S_AXI_GP0_araddr;
  input [1:0]S_AXI_GP0_arburst;
  input [3:0]S_AXI_GP0_arcache;
  input [5:0]S_AXI_GP0_arid;
  input [3:0]S_AXI_GP0_arlen;
  input [1:0]S_AXI_GP0_arlock;
  input [2:0]S_AXI_GP0_arprot;
  input [3:0]S_AXI_GP0_arqos;
  output S_AXI_GP0_arready;
  input [2:0]S_AXI_GP0_arsize;
  input S_AXI_GP0_arvalid;
  input [31:0]S_AXI_GP0_awaddr;
  input [1:0]S_AXI_GP0_awburst;
  input [3:0]S_AXI_GP0_awcache;
  input [5:0]S_AXI_GP0_awid;
  input [3:0]S_AXI_GP0_awlen;
  input [1:0]S_AXI_GP0_awlock;
  input [2:0]S_AXI_GP0_awprot;
  input [3:0]S_AXI_GP0_awqos;
  output S_AXI_GP0_awready;
  input [2:0]S_AXI_GP0_awsize;
  input S_AXI_GP0_awvalid;
  output [5:0]S_AXI_GP0_bid;
  input S_AXI_GP0_bready;
  output [1:0]S_AXI_GP0_bresp;
  output S_AXI_GP0_bvalid;
  output [31:0]S_AXI_GP0_rdata;
  output [5:0]S_AXI_GP0_rid;
  output S_AXI_GP0_rlast;
  input S_AXI_GP0_rready;
  output [1:0]S_AXI_GP0_rresp;
  output S_AXI_GP0_rvalid;
  input [31:0]S_AXI_GP0_wdata;
  input [5:0]S_AXI_GP0_wid;
  input S_AXI_GP0_wlast;
  output S_AXI_GP0_wready;
  input [3:0]S_AXI_GP0_wstrb;
  input S_AXI_GP0_wvalid;

  wire [14:0]DDR_addr;
  wire [2:0]DDR_ba;
  wire DDR_cas_n;
  wire DDR_ck_n;
  wire DDR_ck_p;
  wire DDR_cke;
  wire DDR_cs_n;
  wire [3:0]DDR_dm;
  wire [31:0]DDR_dq;
  wire [3:0]DDR_dqs_n;
  wire [3:0]DDR_dqs_p;
  wire DDR_odt;
  wire DDR_ras_n;
  wire DDR_reset_n;
  wire DDR_we_n;
  wire FCLK_CLK0;
  wire FIXED_IO_ddr_vrn;
  wire FIXED_IO_ddr_vrp;
  wire [53:0]FIXED_IO_mio;
  wire FIXED_IO_ps_clk;
  wire FIXED_IO_ps_porb;
  wire FIXED_IO_ps_srstb;
  wire [63:0]GPIO_I;
  wire [63:0]GPIO_O;
  wire [63:0]GPIO_T;
  wire [31:0]M_AXI_GP0_araddr;
  wire [1:0]M_AXI_GP0_arburst;
  wire [3:0]M_AXI_GP0_arcache;
  wire [11:0]M_AXI_GP0_arid;
  wire [3:0]M_AXI_GP0_arlen;
  wire [1:0]M_AXI_GP0_arlock;
  wire [2:0]M_AXI_GP0_arprot;
  wire [3:0]M_AXI_GP0_arqos;
  wire M_AXI_GP0_arready;
  wire [2:0]M_AXI_GP0_arsize;
  wire M_AXI_GP0_arvalid;
  wire [31:0]M_AXI_GP0_awaddr;
  wire [1:0]M_AXI_GP0_awburst;
  wire [3:0]M_AXI_GP0_awcache;
  wire [11:0]M_AXI_GP0_awid;
  wire [3:0]M_AXI_GP0_awlen;
  wire [1:0]M_AXI_GP0_awlock;
  wire [2:0]M_AXI_GP0_awprot;
  wire [3:0]M_AXI_GP0_awqos;
  wire M_AXI_GP0_awready;
  wire [2:0]M_AXI_GP0_awsize;
  wire M_AXI_GP0_awvalid;
  wire [11:0]M_AXI_GP0_bid;
  wire M_AXI_GP0_bready;
  wire [1:0]M_AXI_GP0_bresp;
  wire M_AXI_GP0_bvalid;
  wire [31:0]M_AXI_GP0_rdata;
  wire [11:0]M_AXI_GP0_rid;
  wire M_AXI_GP0_rlast;
  wire M_AXI_GP0_rready;
  wire [1:0]M_AXI_GP0_rresp;
  wire M_AXI_GP0_rvalid;
  wire [31:0]M_AXI_GP0_wdata;
  wire [11:0]M_AXI_GP0_wid;
  wire M_AXI_GP0_wlast;
  wire M_AXI_GP0_wready;
  wire [3:0]M_AXI_GP0_wstrb;
  wire M_AXI_GP0_wvalid;
  wire [31:0]S_AXI_GP0_araddr;
  wire [1:0]S_AXI_GP0_arburst;
  wire [3:0]S_AXI_GP0_arcache;
  wire [5:0]S_AXI_GP0_arid;
  wire [3:0]S_AXI_GP0_arlen;
  wire [1:0]S_AXI_GP0_arlock;
  wire [2:0]S_AXI_GP0_arprot;
  wire [3:0]S_AXI_GP0_arqos;
  wire S_AXI_GP0_arready;
  wire [2:0]S_AXI_GP0_arsize;
  wire S_AXI_GP0_arvalid;
  wire [31:0]S_AXI_GP0_awaddr;
  wire [1:0]S_AXI_GP0_awburst;
  wire [3:0]S_AXI_GP0_awcache;
  wire [5:0]S_AXI_GP0_awid;
  wire [3:0]S_AXI_GP0_awlen;
  wire [1:0]S_AXI_GP0_awlock;
  wire [2:0]S_AXI_GP0_awprot;
  wire [3:0]S_AXI_GP0_awqos;
  wire S_AXI_GP0_awready;
  wire [2:0]S_AXI_GP0_awsize;
  wire S_AXI_GP0_awvalid;
  wire [5:0]S_AXI_GP0_bid;
  wire S_AXI_GP0_bready;
  wire [1:0]S_AXI_GP0_bresp;
  wire S_AXI_GP0_bvalid;
  wire [31:0]S_AXI_GP0_rdata;
  wire [5:0]S_AXI_GP0_rid;
  wire S_AXI_GP0_rlast;
  wire S_AXI_GP0_rready;
  wire [1:0]S_AXI_GP0_rresp;
  wire S_AXI_GP0_rvalid;
  wire [31:0]S_AXI_GP0_wdata;
  wire [5:0]S_AXI_GP0_wid;
  wire S_AXI_GP0_wlast;
  wire S_AXI_GP0_wready;
  wire [3:0]S_AXI_GP0_wstrb;
  wire S_AXI_GP0_wvalid;

  zynq_ps zynq_ps_i
       (.DDR_addr(DDR_addr),
        .DDR_ba(DDR_ba),
        .DDR_cas_n(DDR_cas_n),
        .DDR_ck_n(DDR_ck_n),
        .DDR_ck_p(DDR_ck_p),
        .DDR_cke(DDR_cke),
        .DDR_cs_n(DDR_cs_n),
        .DDR_dm(DDR_dm),
        .DDR_dq(DDR_dq),
        .DDR_dqs_n(DDR_dqs_n),
        .DDR_dqs_p(DDR_dqs_p),
        .DDR_odt(DDR_odt),
        .DDR_ras_n(DDR_ras_n),
        .DDR_reset_n(DDR_reset_n),
        .DDR_we_n(DDR_we_n),
        .FCLK_CLK0(FCLK_CLK0),
        .FIXED_IO_ddr_vrn(FIXED_IO_ddr_vrn),
        .FIXED_IO_ddr_vrp(FIXED_IO_ddr_vrp),
        .FIXED_IO_mio(FIXED_IO_mio),
        .FIXED_IO_ps_clk(FIXED_IO_ps_clk),
        .FIXED_IO_ps_porb(FIXED_IO_ps_porb),
        .FIXED_IO_ps_srstb(FIXED_IO_ps_srstb),
        .GPIO_I(GPIO_I),
        .GPIO_O(GPIO_O),
        .GPIO_T(GPIO_T),
        .M_AXI_GP0_araddr(M_AXI_GP0_araddr),
        .M_AXI_GP0_arburst(M_AXI_GP0_arburst),
        .M_AXI_GP0_arcache(M_AXI_GP0_arcache),
        .M_AXI_GP0_arid(M_AXI_GP0_arid),
        .M_AXI_GP0_arlen(M_AXI_GP0_arlen),
        .M_AXI_GP0_arlock(M_AXI_GP0_arlock),
        .M_AXI_GP0_arprot(M_AXI_GP0_arprot),
        .M_AXI_GP0_arqos(M_AXI_GP0_arqos),
        .M_AXI_GP0_arready(M_AXI_GP0_arready),
        .M_AXI_GP0_arsize(M_AXI_GP0_arsize),
        .M_AXI_GP0_arvalid(M_AXI_GP0_arvalid),
        .M_AXI_GP0_awaddr(M_AXI_GP0_awaddr),
        .M_AXI_GP0_awburst(M_AXI_GP0_awburst),
        .M_AXI_GP0_awcache(M_AXI_GP0_awcache),
        .M_AXI_GP0_awid(M_AXI_GP0_awid),
        .M_AXI_GP0_awlen(M_AXI_GP0_awlen),
        .M_AXI_GP0_awlock(M_AXI_GP0_awlock),
        .M_AXI_GP0_awprot(M_AXI_GP0_awprot),
        .M_AXI_GP0_awqos(M_AXI_GP0_awqos),
        .M_AXI_GP0_awready(M_AXI_GP0_awready),
        .M_AXI_GP0_awsize(M_AXI_GP0_awsize),
        .M_AXI_GP0_awvalid(M_AXI_GP0_awvalid),
        .M_AXI_GP0_bid(M_AXI_GP0_bid),
        .M_AXI_GP0_bready(M_AXI_GP0_bready),
        .M_AXI_GP0_bresp(M_AXI_GP0_bresp),
        .M_AXI_GP0_bvalid(M_AXI_GP0_bvalid),
        .M_AXI_GP0_rdata(M_AXI_GP0_rdata),
        .M_AXI_GP0_rid(M_AXI_GP0_rid),
        .M_AXI_GP0_rlast(M_AXI_GP0_rlast),
        .M_AXI_GP0_rready(M_AXI_GP0_rready),
        .M_AXI_GP0_rresp(M_AXI_GP0_rresp),
        .M_AXI_GP0_rvalid(M_AXI_GP0_rvalid),
        .M_AXI_GP0_wdata(M_AXI_GP0_wdata),
        .M_AXI_GP0_wid(M_AXI_GP0_wid),
        .M_AXI_GP0_wlast(M_AXI_GP0_wlast),
        .M_AXI_GP0_wready(M_AXI_GP0_wready),
        .M_AXI_GP0_wstrb(M_AXI_GP0_wstrb),
        .M_AXI_GP0_wvalid(M_AXI_GP0_wvalid),
        .S_AXI_GP0_araddr(S_AXI_GP0_araddr),
        .S_AXI_GP0_arburst(S_AXI_GP0_arburst),
        .S_AXI_GP0_arcache(S_AXI_GP0_arcache),
        .S_AXI_GP0_arid(S_AXI_GP0_arid),
        .S_AXI_GP0_arlen(S_AXI_GP0_arlen),
        .S_AXI_GP0_arlock(S_AXI_GP0_arlock),
        .S_AXI_GP0_arprot(S_AXI_GP0_arprot),
        .S_AXI_GP0_arqos(S_AXI_GP0_arqos),
        .S_AXI_GP0_arready(S_AXI_GP0_arready),
        .S_AXI_GP0_arsize(S_AXI_GP0_arsize),
        .S_AXI_GP0_arvalid(S_AXI_GP0_arvalid),
        .S_AXI_GP0_awaddr(S_AXI_GP0_awaddr),
        .S_AXI_GP0_awburst(S_AXI_GP0_awburst),
        .S_AXI_GP0_awcache(S_AXI_GP0_awcache),
        .S_AXI_GP0_awid(S_AXI_GP0_awid),
        .S_AXI_GP0_awlen(S_AXI_GP0_awlen),
        .S_AXI_GP0_awlock(S_AXI_GP0_awlock),
        .S_AXI_GP0_awprot(S_AXI_GP0_awprot),
        .S_AXI_GP0_awqos(S_AXI_GP0_awqos),
        .S_AXI_GP0_awready(S_AXI_GP0_awready),
        .S_AXI_GP0_awsize(S_AXI_GP0_awsize),
        .S_AXI_GP0_awvalid(S_AXI_GP0_awvalid),
        .S_AXI_GP0_bid(S_AXI_GP0_bid),
        .S_AXI_GP0_bready(S_AXI_GP0_bready),
        .S_AXI_GP0_bresp(S_AXI_GP0_bresp),
        .S_AXI_GP0_bvalid(S_AXI_GP0_bvalid),
        .S_AXI_GP0_rdata(S_AXI_GP0_rdata),
        .S_AXI_GP0_rid(S_AXI_GP0_rid),
        .S_AXI_GP0_rlast(S_AXI_GP0_rlast),
        .S_AXI_GP0_rready(S_AXI_GP0_rready),
        .S_AXI_GP0_rresp(S_AXI_GP0_rresp),
        .S_AXI_GP0_rvalid(S_AXI_GP0_rvalid),
        .S_AXI_GP0_wdata(S_AXI_GP0_wdata),
        .S_AXI_GP0_wid(S_AXI_GP0_wid),
        .S_AXI_GP0_wlast(S_AXI_GP0_wlast),
        .S_AXI_GP0_wready(S_AXI_GP0_wready),
        .S_AXI_GP0_wstrb(S_AXI_GP0_wstrb),
        .S_AXI_GP0_wvalid(S_AXI_GP0_wvalid));
endmodule
