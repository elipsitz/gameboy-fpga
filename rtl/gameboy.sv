`timescale 1ns/1ns

/// Main GameBoy module.
/// 
/// Does NOT include the cartridge.
module gameboy (
    /// Clock (4 MHz)
    input clk,
    /// Synchronous reset
    input reset,

    /// Cartridge address select.
    output logic [15:0] cart_addr,
    /// Cartridge access enable.
    output logic        cart_enable,
    /// Cartridge write enable.
    output logic        cart_write,
    /// Cartridge data in.
    input        [7:0]  cart_data_in,
    /// Cartridge data out.
    output logic [7:0]  cart_data_out,

    /// PPU pixel data out.
    output logic [1:0] pixel_out,
    /// Whether the pixel output is valid.
    output logic       pixel_valid
);
    // System Bus.
    logic bus_write = cpu_mem_write;

    // Cartridge.
    assign cart_addr = cpu_mem_addr;
    assign cart_write = cpu_mem_write;
    assign cart_data_out = cpu_mem_data_out;

    // CPU.
    logic [15:0] cpu_mem_addr;
    logic        cpu_mem_enable;
    logic        cpu_mem_write;
    logic [7:0]  cpu_mem_data_in;
    logic [7:0]  cpu_mem_data_out;
    cpu cpu (
        .clk,
        .reset,
        .mem_addr (cpu_mem_addr),
        .mem_enable (cpu_mem_enable),
        .mem_write (cpu_mem_write),
        .mem_data_in (cpu_mem_data_in),
        .mem_data_out (cpu_mem_data_out)
    );

    // PPU.
    logic [6:0]   ppu_ctrl_addr = cpu_mem_addr[6:0];
    logic         ppu_ctrl_enable;
    logic         ppu_ctrl_write = bus_write;
    logic [7:0]   ppu_ctrl_data_in = cpu_mem_data_out;
    logic [7:0]   ppu_ctrl_data_out;
    logic [12:0]  vram_addr = cpu_mem_addr[12:0];
    logic         vram_enable;
    logic         vram_write = bus_write;
    logic [7:0]   vram_data_in = cpu_mem_data_out;
    logic [7:0]   vram_data_out;
    logic [7:0]   oam_addr = cpu_mem_addr[7:0];
    logic         oam_enable;
    logic         oam_write = bus_write;
    logic [7:0]   oam_data_in = cpu_mem_data_out;
    logic [7:0]   oam_data_out;
    ppu ppu (
        .clk,
        .reset,
        .ctrl_addr(ppu_ctrl_addr),
        .ctrl_enable(ppu_ctrl_enable),
        .ctrl_write(ppu_ctrl_write),
        .ctrl_data_in(ppu_ctrl_data_in),
        .ctrl_data_out(ppu_ctrl_data_out),
        .vram_addr,
        .vram_enable,
        .vram_write,
        .vram_data_in,
        .vram_data_out,
        .oam_addr,
        .oam_enable,
        .oam_write,
        .oam_data_in,
        .oam_data_out,
        .pixel_out,
        .pixel_valid
    );

    // Work RAM (WRAM).
    logic [7:0]  work_ram [8191:0]; // DMG: 0xC000 to 0xDFFF
    logic        work_ram_enable;
    logic [7:0]  work_ram_data_out;
    logic [7:0]  work_ram_data_in = cpu_mem_data_out;
    logic [12:0] work_ram_addr = cpu_mem_addr[12:0];
    always_ff @(posedge clk) begin
        if (work_ram_enable) begin
            if (bus_write) work_ram[work_ram_addr] <= work_ram_data_in;
            else work_ram_data_out <= work_ram[work_ram_addr];
        end
    end

    // High RAM (HRAM).
    logic [7:0]  high_ram [127:0]; // But only 127 bytes are accessible.
    logic        high_ram_enable;
    logic [7:0]  high_ram_data_out;
    logic [7:0]  high_ram_data_in = cpu_mem_data_out;
    logic [6:0]  high_ram_addr = cpu_mem_addr[6:0];
    always_ff @(posedge clk) begin
        if (high_ram_enable) begin
            if (bus_write) high_ram[high_ram_addr] <= high_ram_data_in;
            else high_ram_data_out <= high_ram[high_ram_addr];
        end
    end

    // Bus multiplexer.
    always_comb begin
        cpu_mem_data_in = 0;
        cart_enable = 0;
        vram_enable = 0;
        oam_enable = 0;
        work_ram_enable = 0;
        high_ram_enable = 0;
        ppu_ctrl_enable = 0;

        // Cartridge ROM: 0x0000 to 0x7FFF.
        if (cpu_mem_addr <= 16'h7FFF) begin
            cart_enable = cpu_mem_enable;
            cpu_mem_data_in = cart_data_in;
        end
        // Video RAM: 0x8000 to 0x9FFF.
        else if (cpu_mem_addr >= 16'h8000 && cpu_mem_addr <= 16'h9FFF) begin
            vram_enable = cpu_mem_enable;
            cpu_mem_data_in = vram_data_out;
        end
        // Cartridge RAM: 0xA000 to 0xBFFF.
        else if (cpu_mem_addr >= 16'hA000 && cpu_mem_addr <= 16'hBFFF) begin
            cart_enable = cpu_mem_enable;
            cpu_mem_data_in = cart_data_in;
        end
        // Work RAM: 0xC000 to 0xDFFF (and mirror at 0xE000 to 0xFDFF).
        else if (cpu_mem_addr >= 16'hC000 && cpu_mem_addr <= 16'hFDFF) begin
            work_ram_enable = cpu_mem_enable;
            cpu_mem_data_in = work_ram_data_out;
        end
        // Object Attribute Mapping (OAM): 0xFE00 to 0xFE9F.
        else if (cpu_mem_addr >= 16'hFE00 && cpu_mem_addr <= 16'hFE9F) begin
            oam_enable = cpu_mem_enable;
            cpu_mem_data_in = oam_data_out;
        end
        // PPU I/O Registers: 0xFF40 to 0xFF4B.
        else if (cpu_mem_addr >= 16'hFF40 && cpu_mem_addr <= 16'hFF4B) begin
            ppu_ctrl_enable = cpu_mem_enable;
            cpu_mem_data_in = ppu_ctrl_data_out;
        end
        // High RAM: 0xFF80 to 0xFFFE.
        else if (cpu_mem_addr >= 16'hFF80 && cpu_mem_addr <= 16'hFFFE) begin
            high_ram_enable = cpu_mem_enable;
            cpu_mem_data_in = high_ram_data_out;
        end
    end

    // HACK: DEBUG: write serial output to stdout.
    always_ff @(posedge clk) begin
        if (cpu_mem_enable && cpu_mem_write && cpu_mem_addr == 16'hFF01) begin
            $write("%s", cpu_mem_data_out);
        end
    end
endmodule
