module alu (
    /// The first operand.
    input logic [7:0] alu_a,
    /// The second operand.
    input logic [7:0] alu_b,
    /// Operation type.
    input logic [4:0] alu_op,
    /// The input flags (CHNZ).
    input logic [3:0] alu_flag_in,

    /// The ALU output.
    output logic [7:0] alu_out,
    /// The output flags (CHNZ).
    output logic [3:0] alu_flag_out
);
    // Flag indices.
    localparam FLAG_C = 2'd0;
    localparam FLAG_H = 2'd1;
    localparam FLAG_N = 2'd2;
    localparam FLAG_Z = 2'd3;

    // Operations
    localparam OP_ADD  = 5'b00000;
    localparam OP_ADC  = 5'b00001;
    localparam OP_SUB  = 5'b00010;
    localparam OP_SBC  = 5'b00011;
    localparam OP_AND  = 5'b00100;
    localparam OP_XOR  = 5'b00101;
    localparam OP_OR   = 5'b00110;
    localparam OP_CP   = 5'b00111;
    //
    localparam OP_RLCA = 5'b01000;
    localparam OP_RRCA = 5'b01001;
    localparam OP_RLA  = 5'b01010;
    localparam OP_RRA  = 5'b01011;
    localparam OP_DAA  = 5'b01100;
    localparam OP_CPL  = 5'b01101;
    localparam OP_SCF  = 5'b01110;
    localparam OP_CCF  = 5'b01111;
    //
    localparam OP_RLC  = 5'b10000;
    localparam OP_RRC  = 5'b10001;
    localparam OP_RL   = 5'b10010;
    localparam OP_RR   = 5'b10011;
    localparam OP_SLA  = 5'b10100;
    localparam OP_SRA  = 5'b10101;
    localparam OP_SWAP = 5'b10110;
    localparam OP_SRL  = 5'b10111;
    //
    localparam OP_COPY_A = 5'b11000;
    localparam OP_COPY_B = 5'b11001;
    localparam OP_INC_B  = 5'b11010;
    localparam OP_DEC_B  = 5'b11011;
    localparam OP_BIT    = 5'b11100;
    localparam OP_RES    = 5'b11101;
    localparam OP_SET    = 5'b11110;

    logic carry;
    logic [4:0] result_lo;
    logic [4:0] result_hi;
    always @(*) begin
        alu_out = 0;
        alu_flag_out = alu_flag_in;

        case (alu_op)
            OP_COPY_A: alu_out = alu_a;
            OP_COPY_B: alu_out = alu_b;
            OP_INC_B: begin
                alu_out = alu_b + 8'd1;
                alu_flag_out[FLAG_H] = (alu_b[3:0] == 4'b1111);
                alu_flag_out[FLAG_N] = 1'b0;
                alu_flag_out[FLAG_Z] = (alu_out == 8'd0);
            end
            OP_DEC_B: begin
                alu_out = alu_b - 8'd1;
                alu_flag_out[FLAG_H] = (alu_b[3:0] == 4'b0000);
                alu_flag_out[FLAG_N] = 1'b1;
                alu_flag_out[FLAG_Z] = (alu_out == 8'd0);
            end
            OP_ADD, OP_ADC: begin
                carry = (alu_op == OP_ADC) ? alu_flag_in[FLAG_C] : 1'b0;
                result_lo = {1'b0, alu_a[3:0]} + {1'b0, alu_b[3:0]} + {4'b0, carry};
                result_hi = {1'b0, alu_a[7:4]} + {1'b0, alu_b[7:4]} + {4'b0, result_lo[4]};
                alu_out = {result_hi[3:0], result_lo[3:0]};
                alu_flag_out[FLAG_C] = result_hi[4];
                alu_flag_out[FLAG_H] = result_lo[4];
                alu_flag_out[FLAG_N] = 1'b0;
                alu_flag_out[FLAG_Z] = (alu_out == 8'd0);
            end
            OP_SUB, OP_SBC, OP_CP: begin
                carry = (alu_op == OP_SBC) ? alu_flag_in[FLAG_C] : 1'b0;
                result_lo = {1'b0, alu_a[3:0]} + ~({1'b0, alu_b[3:0]}) + {4'b0, ~carry};
                result_hi = {1'b0, alu_a[7:4]} + ~({1'b0, alu_b[7:4]}) + {4'b0, ~result_lo[4]};
                alu_out = (alu_op == OP_CP) ? alu_a : {result_hi[3:0], result_lo[3:0]};
                alu_flag_out[FLAG_C] = result_hi[4];
                alu_flag_out[FLAG_H] = result_lo[4];
                alu_flag_out[FLAG_N] = 1'b1;
                alu_flag_out[FLAG_Z] = (({result_hi[3:0], result_lo[3:0]}) == 8'd0);
            end
            OP_AND: begin
                alu_out = alu_a & alu_b;
                alu_flag_out[FLAG_C] = 1'd0;
                alu_flag_out[FLAG_H] = 1'd1;
                alu_flag_out[FLAG_N] = 1'd0;
                alu_flag_out[FLAG_Z] = (alu_out == 8'd0);
            end
            OP_XOR: begin
                alu_out = alu_a ^ alu_b;
                alu_flag_out[FLAG_C] = 1'd0;
                alu_flag_out[FLAG_H] = 1'd0;
                alu_flag_out[FLAG_N] = 1'd0;
                alu_flag_out[FLAG_Z] = (alu_out == 8'd0);
            end
            OP_OR: begin
                alu_out = alu_a | alu_b;
                alu_flag_out[FLAG_C] = 1'd0;
                alu_flag_out[FLAG_H] = 1'd0;
                alu_flag_out[FLAG_N] = 1'd0;
                alu_flag_out[FLAG_Z] = (alu_out == 8'd0);
            end
            OP_RLCA: begin // Rotate left (NOT through carry)
                alu_out = {alu_a[6:0], alu_a[7]};
                alu_flag_out = {3'b000, alu_a[7]};
            end
            OP_RRCA: begin // Rotate right (NOT through carry)
                alu_out = {alu_a[0], alu_a[7:1]};
                alu_flag_out = {3'b000, alu_a[0]};
            end
            OP_RLA: begin // Rotate left (through carry)
                alu_out = {alu_a[6:0], alu_flag_in[FLAG_C]};
                alu_flag_out = {3'b000, alu_a[7]};
            end
            OP_RRA: begin // Rotate right (through carry)
                alu_out = {alu_flag_in[FLAG_C], alu_a[7:1]};
                alu_flag_out = {3'b000, alu_a[0]};
            end
            OP_DAA: begin // Decimal adjust A
                // TODO ?????
                alu_out = 0;
                alu_flag_out = 4'd0;
            end
            OP_CPL: begin // Complement A
                alu_out = ~alu_a;
                alu_flag_out[FLAG_H] = 1'd1;
                alu_flag_out[FLAG_N] = 1'd1;
            end
            OP_SCF: begin // Set carry flag
                alu_out = alu_a;
                alu_flag_out[FLAG_H] = 1'd0;
                alu_flag_out[FLAG_N] = 1'd0;
                alu_flag_out[FLAG_C] = 1'd1;
            end
            OP_CCF: begin // Complement carry flag
                alu_out = alu_a;
                alu_flag_out[FLAG_H] = 1'd0;
                alu_flag_out[FLAG_N] = 1'd0;
                alu_flag_out[FLAG_C] = ~alu_flag_in[FLAG_C];
            end
            OP_RLC: begin // Rotate left (NOT through carry)
                alu_out = {alu_b[6:0], alu_b[7]};
                alu_flag_out = {(alu_out == 8'd0), 2'b00, alu_b[7]};
            end
            OP_RRC: begin // Rotate right (NOT through carry)
                alu_out = {alu_b[0], alu_b[7:1]};
                alu_flag_out = {(alu_out == 8'd0), 2'b00, alu_b[0]};
            end
            OP_RL: begin // Rotate left (through carry)
                alu_out = {alu_b[6:0], alu_flag_in[FLAG_C]};
                alu_flag_out = {(alu_out == 8'd0), 2'b00, alu_b[7]};
            end
            OP_RR: begin // Rotate right (through carry)
                alu_out = {alu_flag_in[FLAG_C], alu_b[7:1]};
                alu_flag_out = {(alu_out == 8'd0), 2'b00, alu_b[0]};
            end
            OP_SLA: begin
                alu_out = {alu_b[6:0], 1'd0};
                alu_flag_out = {(alu_out == 8'd0), 2'b00, alu_b[7]};
            end
            OP_SRA: begin
                alu_out = {alu_b[7], alu_b[7:1]};
                alu_flag_out = {(alu_out == 8'd0), 2'b00, alu_b[0]};
            end
            OP_SWAP: begin
                alu_out = {alu_b[3:0], alu_b[7:4]};
                alu_flag_out = {(alu_out == 8'd0), 3'b000};
            end
            OP_SRL: begin
                alu_out = {1'd0, alu_b[7:1]};
                alu_flag_out = {(alu_out == 8'd0), 2'b00, alu_b[0]};
            end
        endcase
    end
endmodule