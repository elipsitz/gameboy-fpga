module top_pynq_cartridge_access (
    // Connections to cartridge
    inout [15:0] cartridge_A,
    inout [7:0] cartridge_D,
    inout cartridge_nWR,
    inout cartridge_nRD,
    inout cartridge_nCS,
    inout cartridge_nRST,

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

    /////////////////////////////////////////////////
    // Physical Cartridge I/O
    /////////////////////////////////////////////////
    IOBUF GPIO_iobuf_0 (.IO(cartridge_nWR), .I(GPIO_O[0]), .O(GPIO_I[0]), .T(GPIO_T[0]));
    IOBUF GPIO_iobuf_1 (.IO(cartridge_nRD), .I(GPIO_O[1]), .O(GPIO_I[1]), .T(GPIO_T[1]));
    IOBUF GPIO_iobuf_2 (.IO(cartridge_nCS), .I(GPIO_O[2]), .O(GPIO_I[2]), .T(GPIO_T[2]));
    IOBUF GPIO_iobuf_3 (.IO(cartridge_nRST), .I(GPIO_O[3]), .O(GPIO_I[3]), .T(GPIO_T[3]));

    IOBUF GPIO_iobuf_4 (.IO(cartridge_A[0]), .I(GPIO_O[4]), .O(GPIO_I[4]), .T(GPIO_T[4]));
    IOBUF GPIO_iobuf_5 (.IO(cartridge_A[1]), .I(GPIO_O[5]), .O(GPIO_I[5]), .T(GPIO_T[5]));
    IOBUF GPIO_iobuf_6 (.IO(cartridge_A[2]), .I(GPIO_O[6]), .O(GPIO_I[6]), .T(GPIO_T[6]));
    IOBUF GPIO_iobuf_7 (.IO(cartridge_A[3]), .I(GPIO_O[7]), .O(GPIO_I[7]), .T(GPIO_T[7]));
    IOBUF GPIO_iobuf_8 (.IO(cartridge_A[4]), .I(GPIO_O[8]), .O(GPIO_I[8]), .T(GPIO_T[8]));
    IOBUF GPIO_iobuf_9 (.IO(cartridge_A[5]), .I(GPIO_O[9]), .O(GPIO_I[9]), .T(GPIO_T[9]));
    IOBUF GPIO_iobuf_10 (.IO(cartridge_A[6]), .I(GPIO_O[10]), .O(GPIO_I[10]), .T(GPIO_T[10]));
    IOBUF GPIO_iobuf_11 (.IO(cartridge_A[7]), .I(GPIO_O[11]), .O(GPIO_I[11]), .T(GPIO_T[11]));
    IOBUF GPIO_iobuf_12 (.IO(cartridge_A[8]), .I(GPIO_O[12]), .O(GPIO_I[12]), .T(GPIO_T[12]));
    IOBUF GPIO_iobuf_13 (.IO(cartridge_A[9]), .I(GPIO_O[13]), .O(GPIO_I[13]), .T(GPIO_T[13]));
    IOBUF GPIO_iobuf_14 (.IO(cartridge_A[10]), .I(GPIO_O[14]), .O(GPIO_I[14]), .T(GPIO_T[14]));
    IOBUF GPIO_iobuf_15 (.IO(cartridge_A[11]), .I(GPIO_O[15]), .O(GPIO_I[15]), .T(GPIO_T[15]));
    IOBUF GPIO_iobuf_16 (.IO(cartridge_A[12]), .I(GPIO_O[16]), .O(GPIO_I[16]), .T(GPIO_T[16]));
    IOBUF GPIO_iobuf_17 (.IO(cartridge_A[13]), .I(GPIO_O[17]), .O(GPIO_I[17]), .T(GPIO_T[17]));
    IOBUF GPIO_iobuf_18 (.IO(cartridge_A[14]), .I(GPIO_O[18]), .O(GPIO_I[18]), .T(GPIO_T[18]));
    IOBUF GPIO_iobuf_19 (.IO(cartridge_A[15]), .I(GPIO_O[19]), .O(GPIO_I[19]), .T(GPIO_T[19]));

    IOBUF GPIO_iobuf_20 (.IO(cartridge_D[0]), .I(GPIO_O[20]), .O(GPIO_I[20]), .T(GPIO_T[20]));
    IOBUF GPIO_iobuf_21 (.IO(cartridge_D[1]), .I(GPIO_O[21]), .O(GPIO_I[21]), .T(GPIO_T[21]));
    IOBUF GPIO_iobuf_22 (.IO(cartridge_D[2]), .I(GPIO_O[22]), .O(GPIO_I[22]), .T(GPIO_T[22]));
    IOBUF GPIO_iobuf_23 (.IO(cartridge_D[3]), .I(GPIO_O[23]), .O(GPIO_I[23]), .T(GPIO_T[23]));
    IOBUF GPIO_iobuf_24 (.IO(cartridge_D[4]), .I(GPIO_O[24]), .O(GPIO_I[24]), .T(GPIO_T[24]));
    IOBUF GPIO_iobuf_25 (.IO(cartridge_D[5]), .I(GPIO_O[25]), .O(GPIO_I[25]), .T(GPIO_T[25]));
    IOBUF GPIO_iobuf_26 (.IO(cartridge_D[6]), .I(GPIO_O[26]), .O(GPIO_I[26]), .T(GPIO_T[26]));
    IOBUF GPIO_iobuf_27 (.IO(cartridge_D[7]), .I(GPIO_O[27]), .O(GPIO_I[27]), .T(GPIO_T[27]));


endmodule
