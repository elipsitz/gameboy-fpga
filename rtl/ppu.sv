`timescale 1ns/1ns

/// PPU Mode.
typedef enum logic [1:0] {
    ModeHBlank = 0,
    ModeVBlank = 1,
    ModeOamSearch = 2,
    ModePixelPush = 3
} ppu_mode_e;

/// PPU
module ppu (
    /// Clock (4 MHz)
    input clk,
    /// Synchronous reset
    input reset,

    /// Control register address select.
    input logic  [6:0]  ctrl_addr,
    /// Control register access enable.
    input logic         ctrl_enable,
    /// Control register write enable.
    input logic         ctrl_write,
    /// Control register data in.
    input logic  [7:0]  ctrl_data_in,
    /// Control register data out.
    output logic [7:0]  ctrl_data_out,

    /// VRAM address select.
    input logic  [12:0] vram_addr,
    /// VRAM access enable.
    input logic         vram_enable,
    /// VRAM write enable.
    input logic         vram_write,
    /// VRAM data in.
    input logic  [7:0]  vram_data_in,
    /// VRAM data out.
    output logic [7:0]  vram_data_out,

    /// OAM address select.
    input logic  [7:0]  oam_addr,
    /// OAM access enable.
    input logic         oam_enable,
    /// OAM write enable.
    input logic         oam_write,
    /// OAM data in.
    input logic  [7:0]  oam_data_in,
    /// OAM data out.
    output logic [7:0]  oam_data_out,

    /// Output pixel value.
    output logic [1:0] pixel_out,
    /// Whether the pixel is valid.
    output logic       pixel_valid,
    /// Whether PPU is in HBlank.
    output logic       ppu_hblank,
    /// Whether PPU is in VBlank.
    output logic       ppu_vblank
);
    localparam CYCLES_SCANLINE = 456;
    localparam SCANLINES_VDRAW = 144;
    localparam SCANLINES_VBLANK = 10;

    /// Control registers.
    logic [7:0] reg_lcdc; // LCD Control.
    logic [7:0] reg_stat; // LCD Status.
    logic [7:0] reg_scy;  // Scroll Y.
    logic [7:0] reg_scx;  // Scroll X.
    logic [7:0] reg_ly;   // LCD Y Coordinate.
    logic [7:0] reg_lyc;  // LY Compare.
    logic [7:0] reg_bgp;  // BG Palette Data.
    logic [7:0] reg_obp0; // OBJ Palette 0 Data.
    logic [7:0] reg_obp1; // OBJ Palette 1 Data.
    logic [7:0] reg_wy;   // Window Y Position.
    logic [7:0] reg_wx;   // Window X Position.
    always_ff @(posedge clk) begin
        if (reset) begin
            reg_lcdc <= 0;
            reg_stat <= 0;
            reg_scy <= 0;
            reg_scx <= 0;
            reg_ly <= 0;
            reg_lyc <= 0;
            reg_bgp <= 8'hE4; // Check this?
            reg_obp0 <= 0;
            reg_obp1 <= 0;
            reg_wy <= 0;
            reg_wx <= 0;
        end else if (ctrl_enable) begin
            if (ctrl_write) begin
                ctrl_data_out <= 8'hFF;
                case (ctrl_addr)
                    7'h40: reg_lcdc <= ctrl_data_in;
                    7'h41: reg_stat[6:3] <= ctrl_data_in[6:3];
                    7'h42: reg_scy <= ctrl_data_in;
                    7'h43: reg_scx <= ctrl_data_in;
                    // 43: LY is read only.
                    7'h45: reg_lyc <= ctrl_data_in;
                    // 7'h46: ctrl_data_out <= ;
                    7'h47: reg_bgp <= ctrl_data_in;
                    7'h48: reg_obp0 <= ctrl_data_in;
                    7'h49: reg_obp1 <= ctrl_data_in;
                    7'h4A: reg_wy <= ctrl_data_in;
                    7'h4B: reg_wx <= ctrl_data_in;
                    default: ;
                endcase
            end else begin
                case (ctrl_addr)
                    7'h40: ctrl_data_out <= reg_lcdc;
                    7'h41: ctrl_data_out <= reg_stat; // no bit 7 (0?), 0-2 R only
                    7'h42: ctrl_data_out <= reg_scy;
                    7'h43: ctrl_data_out <= reg_scx;
                    7'h44: ctrl_data_out <= reg_ly; // R only
                    7'h45: ctrl_data_out <= reg_lyc;
                    // 7'h46: ctrl_data_out <= ;
                    7'h47: ctrl_data_out <= reg_bgp;
                    7'h48: ctrl_data_out <= reg_obp0;
                    7'h49: ctrl_data_out <= reg_obp1;
                    7'h4A: ctrl_data_out <= reg_wy;
                    7'h4B: ctrl_data_out <= reg_wx;
                    default: ctrl_data_out <= 8'hFF;
                endcase
            end
        end
    end

    /// Keeping track of scanlines.
    logic [8:0] scanline_cycle; // Current cycle within scanline.
    always_ff @(posedge clk) begin
        if (reset) begin
            scanline_cycle <= 0;
        end else begin
            if (scanline_cycle < CYCLES_SCANLINE - 1) begin
                scanline_cycle <= scanline_cycle + 1;
            end else begin
                scanline_cycle <= 0;
                if (reg_ly < (SCANLINES_VBLANK + SCANLINES_VDRAW) - 1) begin
                    reg_ly <= reg_ly + 1;
                end else begin
                    reg_ly <= 0;
                end
            end
        end
    end

    /// Video RAM.
    logic [7:0] vram [8191:0];
    always_ff @(posedge clk) begin
        // TODO handle conflict when PPU is accessing VRAM.
        if (vram_enable) begin
            if (vram_write) vram[vram_addr] <= vram_data_in;
            else vram_data_out <= vram[vram_addr];
        end
    end

    /// Object Attribute Map.
    logic [7:0] oam [159:0];
    always_ff @(posedge clk) begin
        // TODO handle conflict when PPU is accessing OAM.
        if (oam_enable) begin
            if (oam_write) oam[oam_addr] <= oam_data_in;
            else oam_data_out <= oam[oam_addr];
        end
    end

    /// Render Logic.
    ppu_mode_e mode;
    always_ff @(posedge clk) begin
        if (reset) begin
            mode <= ModeHBlank;
        end
    end

    assign ppu_vblank = reg_ly >= SCANLINES_VDRAW;
    // Dummy pixel output.
    assign ppu_hblank = scanline_cycle >= 368;
    assign pixel_valid = 1;
    assign pixel_out = reg_ly[1:0];
endmodule
