module top_pynq_cartridge_access (
    // Connections to cartridge
    inout [15:0] cartridge_A,
    inout [7:0] cartridge_D,
    inout cartridge_nWR,
    inout cartridge_nRD,
    inout cartridge_nCS,
    inout cartridge_nRST,
    inout cartridge_PHI,
    inout cartridge_VIN,

    // Cartridge shifter control
    inout cartridge_n_oe,
    inout cartridge_dir_ctrl,
    inout cartridge_dir_A_lo,
    inout cartridge_dir_A_hi,
    inout cartridge_dir_D,
    inout cartridge_dir_nRST,
    inout cartridge_dir_VIN,

    // Link cable
    inout link_clock,
    inout link_dir_clock,
    inout link_data,
    inout link_dir_data,
    inout link_in,
    inout link_dir_in,
    inout link_out,
    inout link_dir_out,

    // Switches, buttons, and LEDs on the board
    output [3:0] leds
);
    /////////////////////////////////////////////////
    // Zynq PS
    /////////////////////////////////////////////////
    logic [63:0]GPIO_I;
    logic [63:0]GPIO_O;
    logic [63:0]GPIO_T;

    pynq_cartridge_access_ps pynq_cartridge_access_ps_i(
        .GPIO_I(GPIO_I),
        .GPIO_O(GPIO_O),
        .GPIO_T(GPIO_T)
    );

    assign leds[3:0] = GPIO_O[3:0];

    // assign link_clock = 1'bz;
    // assign link_dir_clock = 1'b0;
    // assign link_data = 1'bz;
    // assign link_dir_data = 1'b0;
    // assign link_in = 1'bz;
    // assign link_dir_in = 1'b0;
    // assign link_out = 1'bz;
    // assign link_dir_out = 1'b0;

    /////////////////////////////////////////////////
    // Physical Cartridge I/O
    /////////////////////////////////////////////////
    IOBUF GPIO_iobuf_1 (.IO(cartridge_nWR), .I(GPIO_O[1]), .O(GPIO_I[1]), .T(GPIO_T[1]));
    IOBUF GPIO_iobuf_2 (.IO(cartridge_nRD), .I(GPIO_O[2]), .O(GPIO_I[2]), .T(GPIO_T[2]));
    IOBUF GPIO_iobuf_3 (.IO(cartridge_nCS), .I(GPIO_O[3]), .O(GPIO_I[3]), .T(GPIO_T[3]));
    IOBUF GPIO_iobuf_4 (.IO(cartridge_nRST), .I(GPIO_O[4]), .O(GPIO_I[4]), .T(GPIO_T[4]));
    IOBUF GPIO_iobuf_5 (.IO(cartridge_PHI), .I(GPIO_O[5]), .O(GPIO_I[5]), .T(GPIO_T[5]));
    IOBUF GPIO_iobuf_6 (.IO(cartridge_VIN), .I(GPIO_O[6]), .O(GPIO_I[6]), .T(GPIO_T[6]));

    // When DIR is high, it's OUTPUT. When DIR is low, it's INPUT (relative to Pynq).
    IOBUF GPIO_iobuf_7 (.IO(cartridge_n_oe), .I(GPIO_O[7]), .O(GPIO_I[7]), .T(GPIO_T[7]));
    IOBUF GPIO_iobuf_8 (.IO(cartridge_dir_A_hi), .I(GPIO_O[8]), .O(GPIO_I[8]), .T(GPIO_T[8]));
    IOBUF GPIO_iobuf_9 (.IO(cartridge_dir_A_lo), .I(GPIO_O[9]), .O(GPIO_I[9]), .T(GPIO_T[9]));
    IOBUF GPIO_iobuf_10 (.IO(cartridge_dir_ctrl), .I(GPIO_O[10]), .O(GPIO_I[10]), .T(GPIO_T[10]));
    IOBUF GPIO_iobuf_11 (.IO(cartridge_dir_D), .I(GPIO_O[11]), .O(GPIO_I[11]), .T(GPIO_T[11]));
    IOBUF GPIO_iobuf_12 (.IO(cartridge_dir_nRST), .I(GPIO_O[12]), .O(GPIO_I[12]), .T(GPIO_T[12]));
    IOBUF GPIO_iobuf_13 (.IO(cartridge_dir_VIN), .I(GPIO_O[13]), .O(GPIO_I[13]), .T(GPIO_T[13]));

    IOBUF GPIO_iobuf_14 (.IO(cartridge_A[0]), .I(GPIO_O[14]), .O(GPIO_I[14]), .T(GPIO_T[14]));
    IOBUF GPIO_iobuf_15 (.IO(cartridge_A[1]), .I(GPIO_O[15]), .O(GPIO_I[15]), .T(GPIO_T[15]));
    IOBUF GPIO_iobuf_16 (.IO(cartridge_A[2]), .I(GPIO_O[16]), .O(GPIO_I[16]), .T(GPIO_T[16]));
    IOBUF GPIO_iobuf_17 (.IO(cartridge_A[3]), .I(GPIO_O[17]), .O(GPIO_I[17]), .T(GPIO_T[17]));
    IOBUF GPIO_iobuf_18 (.IO(cartridge_A[4]), .I(GPIO_O[18]), .O(GPIO_I[18]), .T(GPIO_T[18]));
    IOBUF GPIO_iobuf_19 (.IO(cartridge_A[5]), .I(GPIO_O[19]), .O(GPIO_I[19]), .T(GPIO_T[19]));
    IOBUF GPIO_iobuf_20 (.IO(cartridge_A[6]), .I(GPIO_O[20]), .O(GPIO_I[20]), .T(GPIO_T[20]));
    IOBUF GPIO_iobuf_21 (.IO(cartridge_A[7]), .I(GPIO_O[21]), .O(GPIO_I[21]), .T(GPIO_T[21]));
    IOBUF GPIO_iobuf_22 (.IO(cartridge_A[8]), .I(GPIO_O[22]), .O(GPIO_I[22]), .T(GPIO_T[22]));
    IOBUF GPIO_iobuf_23 (.IO(cartridge_A[9]), .I(GPIO_O[23]), .O(GPIO_I[23]), .T(GPIO_T[23]));
    IOBUF GPIO_iobuf_24 (.IO(cartridge_A[10]), .I(GPIO_O[24]), .O(GPIO_I[24]), .T(GPIO_T[24]));
    IOBUF GPIO_iobuf_25 (.IO(cartridge_A[11]), .I(GPIO_O[25]), .O(GPIO_I[25]), .T(GPIO_T[25]));
    IOBUF GPIO_iobuf_26 (.IO(cartridge_A[12]), .I(GPIO_O[26]), .O(GPIO_I[26]), .T(GPIO_T[26]));
    IOBUF GPIO_iobuf_27 (.IO(cartridge_A[13]), .I(GPIO_O[27]), .O(GPIO_I[27]), .T(GPIO_T[27]));
    IOBUF GPIO_iobuf_28 (.IO(cartridge_A[14]), .I(GPIO_O[28]), .O(GPIO_I[28]), .T(GPIO_T[28]));
    IOBUF GPIO_iobuf_29 (.IO(cartridge_A[15]), .I(GPIO_O[29]), .O(GPIO_I[29]), .T(GPIO_T[29]));

    IOBUF GPIO_iobuf_30 (.IO(cartridge_D[0]), .I(GPIO_O[30]), .O(GPIO_I[30]), .T(GPIO_T[30]));
    IOBUF GPIO_iobuf_31 (.IO(cartridge_D[1]), .I(GPIO_O[31]), .O(GPIO_I[31]), .T(GPIO_T[31]));
    IOBUF GPIO_iobuf_32 (.IO(cartridge_D[2]), .I(GPIO_O[32]), .O(GPIO_I[32]), .T(GPIO_T[32]));
    IOBUF GPIO_iobuf_33 (.IO(cartridge_D[3]), .I(GPIO_O[33]), .O(GPIO_I[33]), .T(GPIO_T[33]));
    IOBUF GPIO_iobuf_34 (.IO(cartridge_D[4]), .I(GPIO_O[34]), .O(GPIO_I[34]), .T(GPIO_T[34]));
    IOBUF GPIO_iobuf_35 (.IO(cartridge_D[5]), .I(GPIO_O[35]), .O(GPIO_I[35]), .T(GPIO_T[35]));
    IOBUF GPIO_iobuf_36 (.IO(cartridge_D[6]), .I(GPIO_O[36]), .O(GPIO_I[36]), .T(GPIO_T[36]));
    IOBUF GPIO_iobuf_37 (.IO(cartridge_D[7]), .I(GPIO_O[37]), .O(GPIO_I[37]), .T(GPIO_T[37]));


    IOBUF GPIO_iobuf_38 (.IO(link_clock), .I(GPIO_O[38]), .O(GPIO_I[38]), .T(GPIO_T[38]));
    IOBUF GPIO_iobuf_39 (.IO(link_dir_clock), .I(GPIO_O[39]), .O(GPIO_I[39]), .T(GPIO_T[39]));
    IOBUF GPIO_iobuf_40 (.IO(link_data), .I(GPIO_O[40]), .O(GPIO_I[40]), .T(GPIO_T[40]));
    IOBUF GPIO_iobuf_41 (.IO(link_dir_data), .I(GPIO_O[41]), .O(GPIO_I[41]), .T(GPIO_T[41]));
    IOBUF GPIO_iobuf_42 (.IO(link_in), .I(GPIO_O[42]), .O(GPIO_I[42]), .T(GPIO_T[42]));
    IOBUF GPIO_iobuf_43 (.IO(link_dir_in), .I(GPIO_O[43]), .O(GPIO_I[43]), .T(GPIO_T[43]));
    IOBUF GPIO_iobuf_44 (.IO(link_out), .I(GPIO_O[44]), .O(GPIO_I[44]), .T(GPIO_T[44]));
    IOBUF GPIO_iobuf_45 (.IO(link_dir_out), .I(GPIO_O[45]), .O(GPIO_I[45]), .T(GPIO_T[45]));

endmodule
