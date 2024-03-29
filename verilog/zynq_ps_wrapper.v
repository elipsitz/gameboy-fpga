//Copyright 1986-2020 Xilinx, Inc. All Rights Reserved.
//--------------------------------------------------------------------------------
//Tool Version: Vivado v.2020.2 (lin64) Build 3064766 Wed Nov 18 09:12:47 MST 2020
//Date        : Sat Jun 10 15:09:03 2023
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
    FIXED_IO_ddr_vrn,
    FIXED_IO_ddr_vrp,
    FIXED_IO_mio,
    FIXED_IO_ps_clk,
    FIXED_IO_ps_porb,
    FIXED_IO_ps_srstb,
    GPIO_I,
    GPIO_O,
    GPIO_T,
    IIC_0_0_scl_io,
    IIC_0_0_sda_io,
    M_AXI_0_araddr,
    M_AXI_0_arprot,
    M_AXI_0_arready,
    M_AXI_0_arvalid,
    M_AXI_0_awaddr,
    M_AXI_0_awprot,
    M_AXI_0_awready,
    M_AXI_0_awvalid,
    M_AXI_0_bready,
    M_AXI_0_bresp,
    M_AXI_0_bvalid,
    M_AXI_0_rdata,
    M_AXI_0_rready,
    M_AXI_0_rresp,
    M_AXI_0_rvalid,
    M_AXI_0_wdata,
    M_AXI_0_wready,
    M_AXI_0_wstrb,
    M_AXI_0_wvalid,
    S_AXI_0_araddr,
    S_AXI_0_arburst,
    S_AXI_0_arcache,
    S_AXI_0_arid,
    S_AXI_0_arlen,
    S_AXI_0_arlock,
    S_AXI_0_arprot,
    S_AXI_0_arqos,
    S_AXI_0_arready,
    S_AXI_0_arsize,
    S_AXI_0_arvalid,
    S_AXI_0_awaddr,
    S_AXI_0_awburst,
    S_AXI_0_awcache,
    S_AXI_0_awid,
    S_AXI_0_awlen,
    S_AXI_0_awlock,
    S_AXI_0_awprot,
    S_AXI_0_awqos,
    S_AXI_0_awready,
    S_AXI_0_awsize,
    S_AXI_0_awvalid,
    S_AXI_0_bid,
    S_AXI_0_bready,
    S_AXI_0_bresp,
    S_AXI_0_bvalid,
    S_AXI_0_rdata,
    S_AXI_0_rid,
    S_AXI_0_rlast,
    S_AXI_0_rready,
    S_AXI_0_rresp,
    S_AXI_0_rvalid,
    S_AXI_0_wdata,
    S_AXI_0_wid,
    S_AXI_0_wlast,
    S_AXI_0_wready,
    S_AXI_0_wstrb,
    S_AXI_0_wvalid,
    clk_8mhz,
    clk_axi_dram,
    clk_pixel,
    clk_pixel_x5,
    pll_0_locked,
    pll_1_locked,
    reset_8mhz);
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
  inout FIXED_IO_ddr_vrn;
  inout FIXED_IO_ddr_vrp;
  inout [53:0]FIXED_IO_mio;
  inout FIXED_IO_ps_clk;
  inout FIXED_IO_ps_porb;
  inout FIXED_IO_ps_srstb;
  input [63:0]GPIO_I;
  output [63:0]GPIO_O;
  output [63:0]GPIO_T;
  inout IIC_0_0_scl_io;
  inout IIC_0_0_sda_io;
  output [31:0]M_AXI_0_araddr;
  output [2:0]M_AXI_0_arprot;
  input M_AXI_0_arready;
  output M_AXI_0_arvalid;
  output [31:0]M_AXI_0_awaddr;
  output [2:0]M_AXI_0_awprot;
  input M_AXI_0_awready;
  output M_AXI_0_awvalid;
  output M_AXI_0_bready;
  input [1:0]M_AXI_0_bresp;
  input M_AXI_0_bvalid;
  input [31:0]M_AXI_0_rdata;
  output M_AXI_0_rready;
  input [1:0]M_AXI_0_rresp;
  input M_AXI_0_rvalid;
  output [31:0]M_AXI_0_wdata;
  input M_AXI_0_wready;
  output [3:0]M_AXI_0_wstrb;
  output M_AXI_0_wvalid;
  input [31:0]S_AXI_0_araddr;
  input [1:0]S_AXI_0_arburst;
  input [3:0]S_AXI_0_arcache;
  input [5:0]S_AXI_0_arid;
  input [3:0]S_AXI_0_arlen;
  input [1:0]S_AXI_0_arlock;
  input [2:0]S_AXI_0_arprot;
  input [3:0]S_AXI_0_arqos;
  output S_AXI_0_arready;
  input [2:0]S_AXI_0_arsize;
  input S_AXI_0_arvalid;
  input [31:0]S_AXI_0_awaddr;
  input [1:0]S_AXI_0_awburst;
  input [3:0]S_AXI_0_awcache;
  input [5:0]S_AXI_0_awid;
  input [3:0]S_AXI_0_awlen;
  input [1:0]S_AXI_0_awlock;
  input [2:0]S_AXI_0_awprot;
  input [3:0]S_AXI_0_awqos;
  output S_AXI_0_awready;
  input [2:0]S_AXI_0_awsize;
  input S_AXI_0_awvalid;
  output [5:0]S_AXI_0_bid;
  input S_AXI_0_bready;
  output [1:0]S_AXI_0_bresp;
  output S_AXI_0_bvalid;
  output [63:0]S_AXI_0_rdata;
  output [5:0]S_AXI_0_rid;
  output S_AXI_0_rlast;
  input S_AXI_0_rready;
  output [1:0]S_AXI_0_rresp;
  output S_AXI_0_rvalid;
  input [63:0]S_AXI_0_wdata;
  input [5:0]S_AXI_0_wid;
  input S_AXI_0_wlast;
  output S_AXI_0_wready;
  input [7:0]S_AXI_0_wstrb;
  input S_AXI_0_wvalid;
  output clk_8mhz;
  output clk_axi_dram;
  output clk_pixel;
  output clk_pixel_x5;
  output pll_0_locked;
  output pll_1_locked;
  output [0:0]reset_8mhz;

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
  wire FIXED_IO_ddr_vrn;
  wire FIXED_IO_ddr_vrp;
  wire [53:0]FIXED_IO_mio;
  wire FIXED_IO_ps_clk;
  wire FIXED_IO_ps_porb;
  wire FIXED_IO_ps_srstb;
  wire [63:0]GPIO_I;
  wire [63:0]GPIO_O;
  wire [63:0]GPIO_T;
  wire IIC_0_0_scl_i;
  wire IIC_0_0_scl_io;
  wire IIC_0_0_scl_o;
  wire IIC_0_0_scl_t;
  wire IIC_0_0_sda_i;
  wire IIC_0_0_sda_io;
  wire IIC_0_0_sda_o;
  wire IIC_0_0_sda_t;
  wire [31:0]M_AXI_0_araddr;
  wire [2:0]M_AXI_0_arprot;
  wire M_AXI_0_arready;
  wire M_AXI_0_arvalid;
  wire [31:0]M_AXI_0_awaddr;
  wire [2:0]M_AXI_0_awprot;
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
  wire [3:0]M_AXI_0_wstrb;
  wire M_AXI_0_wvalid;
  wire [31:0]S_AXI_0_araddr;
  wire [1:0]S_AXI_0_arburst;
  wire [3:0]S_AXI_0_arcache;
  wire [5:0]S_AXI_0_arid;
  wire [3:0]S_AXI_0_arlen;
  wire [1:0]S_AXI_0_arlock;
  wire [2:0]S_AXI_0_arprot;
  wire [3:0]S_AXI_0_arqos;
  wire S_AXI_0_arready;
  wire [2:0]S_AXI_0_arsize;
  wire S_AXI_0_arvalid;
  wire [31:0]S_AXI_0_awaddr;
  wire [1:0]S_AXI_0_awburst;
  wire [3:0]S_AXI_0_awcache;
  wire [5:0]S_AXI_0_awid;
  wire [3:0]S_AXI_0_awlen;
  wire [1:0]S_AXI_0_awlock;
  wire [2:0]S_AXI_0_awprot;
  wire [3:0]S_AXI_0_awqos;
  wire S_AXI_0_awready;
  wire [2:0]S_AXI_0_awsize;
  wire S_AXI_0_awvalid;
  wire [5:0]S_AXI_0_bid;
  wire S_AXI_0_bready;
  wire [1:0]S_AXI_0_bresp;
  wire S_AXI_0_bvalid;
  wire [63:0]S_AXI_0_rdata;
  wire [5:0]S_AXI_0_rid;
  wire S_AXI_0_rlast;
  wire S_AXI_0_rready;
  wire [1:0]S_AXI_0_rresp;
  wire S_AXI_0_rvalid;
  wire [63:0]S_AXI_0_wdata;
  wire [5:0]S_AXI_0_wid;
  wire S_AXI_0_wlast;
  wire S_AXI_0_wready;
  wire [7:0]S_AXI_0_wstrb;
  wire S_AXI_0_wvalid;
  wire clk_8mhz;
  wire clk_axi_dram;
  wire clk_pixel;
  wire clk_pixel_x5;
  wire pll_0_locked;
  wire pll_1_locked;
  wire [0:0]reset_8mhz;

  IOBUF IIC_0_0_scl_iobuf
       (.I(IIC_0_0_scl_o),
        .IO(IIC_0_0_scl_io),
        .O(IIC_0_0_scl_i),
        .T(IIC_0_0_scl_t));
  IOBUF IIC_0_0_sda_iobuf
       (.I(IIC_0_0_sda_o),
        .IO(IIC_0_0_sda_io),
        .O(IIC_0_0_sda_i),
        .T(IIC_0_0_sda_t));
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
        .FIXED_IO_ddr_vrn(FIXED_IO_ddr_vrn),
        .FIXED_IO_ddr_vrp(FIXED_IO_ddr_vrp),
        .FIXED_IO_mio(FIXED_IO_mio),
        .FIXED_IO_ps_clk(FIXED_IO_ps_clk),
        .FIXED_IO_ps_porb(FIXED_IO_ps_porb),
        .FIXED_IO_ps_srstb(FIXED_IO_ps_srstb),
        .GPIO_I(GPIO_I),
        .GPIO_O(GPIO_O),
        .GPIO_T(GPIO_T),
        .IIC_0_0_scl_i(IIC_0_0_scl_i),
        .IIC_0_0_scl_o(IIC_0_0_scl_o),
        .IIC_0_0_scl_t(IIC_0_0_scl_t),
        .IIC_0_0_sda_i(IIC_0_0_sda_i),
        .IIC_0_0_sda_o(IIC_0_0_sda_o),
        .IIC_0_0_sda_t(IIC_0_0_sda_t),
        .M_AXI_0_araddr(M_AXI_0_araddr),
        .M_AXI_0_arprot(M_AXI_0_arprot),
        .M_AXI_0_arready(M_AXI_0_arready),
        .M_AXI_0_arvalid(M_AXI_0_arvalid),
        .M_AXI_0_awaddr(M_AXI_0_awaddr),
        .M_AXI_0_awprot(M_AXI_0_awprot),
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
        .M_AXI_0_wstrb(M_AXI_0_wstrb),
        .M_AXI_0_wvalid(M_AXI_0_wvalid),
        .S_AXI_0_araddr(S_AXI_0_araddr),
        .S_AXI_0_arburst(S_AXI_0_arburst),
        .S_AXI_0_arcache(S_AXI_0_arcache),
        .S_AXI_0_arid(S_AXI_0_arid),
        .S_AXI_0_arlen(S_AXI_0_arlen),
        .S_AXI_0_arlock(S_AXI_0_arlock),
        .S_AXI_0_arprot(S_AXI_0_arprot),
        .S_AXI_0_arqos(S_AXI_0_arqos),
        .S_AXI_0_arready(S_AXI_0_arready),
        .S_AXI_0_arsize(S_AXI_0_arsize),
        .S_AXI_0_arvalid(S_AXI_0_arvalid),
        .S_AXI_0_awaddr(S_AXI_0_awaddr),
        .S_AXI_0_awburst(S_AXI_0_awburst),
        .S_AXI_0_awcache(S_AXI_0_awcache),
        .S_AXI_0_awid(S_AXI_0_awid),
        .S_AXI_0_awlen(S_AXI_0_awlen),
        .S_AXI_0_awlock(S_AXI_0_awlock),
        .S_AXI_0_awprot(S_AXI_0_awprot),
        .S_AXI_0_awqos(S_AXI_0_awqos),
        .S_AXI_0_awready(S_AXI_0_awready),
        .S_AXI_0_awsize(S_AXI_0_awsize),
        .S_AXI_0_awvalid(S_AXI_0_awvalid),
        .S_AXI_0_bid(S_AXI_0_bid),
        .S_AXI_0_bready(S_AXI_0_bready),
        .S_AXI_0_bresp(S_AXI_0_bresp),
        .S_AXI_0_bvalid(S_AXI_0_bvalid),
        .S_AXI_0_rdata(S_AXI_0_rdata),
        .S_AXI_0_rid(S_AXI_0_rid),
        .S_AXI_0_rlast(S_AXI_0_rlast),
        .S_AXI_0_rready(S_AXI_0_rready),
        .S_AXI_0_rresp(S_AXI_0_rresp),
        .S_AXI_0_rvalid(S_AXI_0_rvalid),
        .S_AXI_0_wdata(S_AXI_0_wdata),
        .S_AXI_0_wid(S_AXI_0_wid),
        .S_AXI_0_wlast(S_AXI_0_wlast),
        .S_AXI_0_wready(S_AXI_0_wready),
        .S_AXI_0_wstrb(S_AXI_0_wstrb),
        .S_AXI_0_wvalid(S_AXI_0_wvalid),
        .clk_8mhz(clk_8mhz),
        .clk_axi_dram(clk_axi_dram),
        .clk_pixel(clk_pixel),
        .clk_pixel_x5(clk_pixel_x5),
        .pll_0_locked(pll_0_locked),
        .pll_1_locked(pll_1_locked),
        .reset_8mhz(reset_8mhz));
endmodule
