typedef enum logic [1:0] {
    MicroBranchNext,
    MicroBranchJump,
    MicroBranchCond,
    MicroBranchDispatch
} microbranch_e;

typedef enum logic [0:0] {
    DispatchPrefixNone = 1'b0,
    DispatchPrefixCB = 1'b1
} dispatch_prefix_e;

typedef enum logic [1:0] {
    IMEUpdateSame,
    IMEUpdateEnable,
    IMEUpdateDisable
} ime_update_e;

typedef logic [7:0] state_t;

module cpu_control (
    /// Clock
    input clk,
    input [1:0] t_cycle,
    /// Synchronous reset
    input reset,

    /// Current memory data in.
    input [7:0] mem_data_in,
    /// Whether the current condition (in instruction register) is satisfied (based on flags)
    input condition,

    /// Control signal: how PC should be updated.
    output pc_next_e pc_next,
    /// Control signal: if the instruction reg. should be loaded with memory read.
    output logic inst_load,
    /// Control signal: the first register we're reading.
    output reg_sel_e reg_read1_sel,
    /// Control signal: the second register we're reading.
    output reg_sel_e reg_read2_sel,
    /// Control signal: the register we're (maybe) writing to.
    output reg_sel_e reg_write_sel,
    /// Control signal: register write operation to perform.
    output reg_op_e reg_op,
    /// Control signal: incrementer/decrementer operation to perform.
    output inc_op_e inc_op,
    /// Control signal: incrementer/decrementer register target.
    output inc_reg_e inc_reg,
    /// Control signal: ALU operation.
    output alu_op_e alu_op,
    /// Control signal: ALU select A.
    output alu_sel_a_e alu_sel_a,
    /// Control signal: ALU select B.
    output alu_sel_b_e alu_sel_b,
    /// Control signal: whether ALU should update flags.
    output alu_flag_set_e alu_flag_set,
    /// Control signal: whether we're accessing memory.
    output logic mem_enable,
    /// Control signal: whether we're writing to memory (if `mem_enable`). 
    output logic mem_write,
    /// Control signal: where the memory address comes from.
    output mem_addr_sel_e mem_addr_sel
);
    state_t state = 0; // Initial state = NOP
    logic ime = 0; // Interrupt master enable.

    // Describe control given the state.
    dispatch_prefix_e dispatch_prefix; // 0 for regular, 1 for CB.
    microbranch_e microbranch;
    state_t next_state;
    ime_update_e ime_update;
    always_comb begin
        // TODO: make these don't cares?
        pc_next = PcNextSame;
        inst_load = 0;
        reg_read1_sel = RegSelA;
        reg_read2_sel = RegSelA;
        reg_write_sel = RegSelA;
        reg_op = RegOpNone;
        inc_op = IncOpNone;
        inc_reg = IncRegPC;
        alu_op = AluOpCopyA;
        alu_sel_a = AluSelARegA;
        alu_sel_b = AluSelBReg2;
        alu_flag_set = AluFlagSetNone;
        mem_enable = 0;
        mem_write = 0;
        mem_addr_sel = MemAddrSelIncrementer;
        microbranch = MicroBranchNext;
        next_state = 0;
        dispatch_prefix = DispatchPrefixNone;
        ime_update = IMEUpdateSame;
        
        case (state)
            `include "cpu_control_signals.inc"
        endcase
    end

    // Describe next state given current state.
    always_ff @(posedge clk) begin
        if (reset) begin
            state <= 0;
            ime <= 1; // TODO check what the initial value should be
        end else if (t_cycle == 3) begin
            // Advance the state machine.
            if (microbranch == MicroBranchNext) state <= state + 1;
            else if (microbranch == MicroBranchDispatch) begin
                // Dispatch based off the next memory we're reading.
                casez ({dispatch_prefix, mem_data_in})
                    `include "cpu_control_dispatch.inc"
                    default: state <= 1; // "INVALID" state.
                endcase
            end else if (microbranch == MicroBranchJump) state <= next_state;
            else if (microbranch == MicroBranchCond) state <= (condition ? next_state : (state + 1));

            // Maybe enable/disable IME.
            case (ime_update)
                IMEUpdateEnable: ime <= 1;
                IMEUpdateDisable: ime <= 0;
            endcase
        end
    end


endmodule