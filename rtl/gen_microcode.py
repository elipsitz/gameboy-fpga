import csv
from pathlib import Path
import math

BINARY = {"Yes": "1", "No": "0"}
REG_SELECT = {
    "A": "RegSelA",
    "C": "RegSelC",
    "W": "RegSelW",
    "Z": "RegSelZ",
    "H": "RegSelH",
    "L": "RegSelL",
    "Reg8Src": "RegSelReg8Src",
    "Reg8Dest": "RegSelReg8Dest",
    "Reg16Hi": "RegSelReg16Hi",
    "Reg16Lo": "RegSelReg16Lo",
    "SP_Hi": "RegSPHi",
    "SP_Lo": "RegSPLo",
    "PC_Hi": "RegPCHi",
    "PC_Lo": "RegPCLo",
}
SIGNALS = {
    "Label": None,
    "Encoding": None,
    "Comment": None,
    "NextState": None,
    "PcNext": {
        "Same": "PcNextSame",
        "IncOut": "PcNextIncOut",
    },
    "RegRead1Sel": REG_SELECT,
    "RegRead2Sel": REG_SELECT,
    "RegWriteSel": REG_SELECT,
    "RegOp": {
        "None": "RegOpNone",
        "WriteAlu": "RegOpWriteAlu",
        "WriteMem": "RegOpWriteMem",
    },
    "IncOp": {
        "No": "IncOpNone",
        "Inc": "IncOpInc",
        "Dec": "IncOpDec",
        "IncNoWrite": "IncOpIncNoWrite",
    },
    "IncReg": {
        "PC": "IncRegPC",
        "HL": "IncRegHL",
        "SP": "IncRegSP",
        "WZ": "IncRegWZ",
        "Inst16": "IncRegInst16",
    },
    "AluOp": {
        "CopyA": "AluOpCopyA",
        "CopyB": "AluOpCopyB",
        "IncA": "AluOpIncA",
        "InstAlu": "AluOpInstAlu",
        "AddLo": "AluOpAddLo",
        "AddHi": "AluOpAddHi",
    },
    "AluSelA": {
        "A": "AluSelARegA",
        "Reg1": "AluSelAReg1",
    },
    "AluSelB": {
        "Reg2": "AluSelBReg2",
        "SignReg2": "AluSelBSignReg2",
    },
    "AluFlagSet": {
        "No": "AluFlagSetNone",
        "All": "AluFlagSetAll",
        "-***": "AluFlagSet_NHC",
        "0***": "AluFlagSet0NHC",
    },
    "MemEn": BINARY,
    "MemWr": BINARY,
    "MemAddrSel": {
        "Incrementer": "MemAddrSelIncrementer",
        "High": "MemAddrSelHigh",
    },
    "MicroBranch": {
        "Next": "MicroBranchNext",
        "Jump": "MicroBranchJump",
        "Cond": "MicroBranchCond",
        "Fetch": "MicroBranchDispatch",
        "Fetch*": "MicroBranchDispatch",  # Fetch, but don't force all of the fields
    },
    "InstLoad": BINARY,
}


class State:
    def __init__(self, i, row):
        self.i = i
        self.data = row
        self.label = self.data["Label"] if self.data["Label"] else None

        # Basic validation
        for k, v in self.data.items():
            if k not in SIGNALS:
                raise Exception(f"Error on row {i}: Unknown signal key '{k}'")
            valid_values = SIGNALS[k]
            if valid_values is not None and (v not in valid_values) and v != "-":
                raise Exception(f"Error on row {i}: Invalid value '{v}' for key '{k}'")
        if self.data["Encoding"] and len(self.data["Encoding"]) != 8:
            raise Exception(f"Error on row {i}: bad 'Encoding'")
        if self.data["MemWr"] == "Yes" and self.data["AluOp"] == "-":
            raise Exception(f"Row {i}: AluOp must be set if MemWr is Yes")
        if self.data["MemAddrSel"] == "Incrementer" and self.data["IncReg"] == "-":
            raise Exception(f"Row {i}: IncReg must be set if MemAddrSel is Incrementer")

        if self.data["MicroBranch"] in ("Fetch", "Fetch*"):
            forced = {
                "PcNext": "IncOut",
                "MemEn": "Yes",
                "MemWr": "No",
                "MemAddrSel": "Incrementer",
                "IncOp": "Inc",
                "IncReg": "PC",
                "NextState": "-",
                "InstLoad": "Yes",
            }
            for k, v in forced.items():
                if self.data.get(k, "-") == "-":
                    self.data[k] = v
                elif self.data["MicroBranch"] == "Fetch":
                    raise Exception(f"Error on row {i}: {k} must be '-' w/ Fetch")
        else:
            self.data["InstLoad"] = "No"

    def get(self, key):
        if self.data[key] == "-":
            return None
        return SIGNALS[key][self.data[key]]


def simple_signal(f, state, key, wire_name):
    if (val := state.get(key)) is not None:
        f.write(f"    {wire_name} = {val};\n")


if __name__ == "__main__":
    states = []

    # Read microcode CSV.
    path = Path(__file__).parent / "microcode.csv"
    with open(path) as file:
        reader = csv.DictReader(file)
        for i, row in enumerate(reader):
            states.append(State(i, row))

    state_width = int(max(1, math.ceil(math.log2(len(states)))))
    print("State width={} ({}:0)".format(state_width, state_width - 1))

    state_labels = {s.label: s.i for s in states if s.label}
    for s in states:
        next_state = s.data["NextState"]
        if next_state != "-":
            if next_state not in state_labels:
                raise Exception(f"Error row {s.i}: Invalid next state '{next_state}'")
            s.next_state = state_labels[next_state]
        else:
            s.next_state = None

    # Warnings about unused signals.
    for signal_name, values in SIGNALS.items():
        seen = set()
        for s in states:
            if s.data.get(signal_name, "-") != "-":
                seen.add(s.data[signal_name])
        if values is not None:
            allowed = set(values.keys())
            never_seen = allowed - seen
            if never_seen:
                print(
                    f"WARNING: Never seen allowed signals for '{signal_name}':",
                    " ".join(sorted(list(never_seen))),
                )

    # Write control signals output file.
    path = Path(__file__).parent / "cpu_control_signals.inc"
    with open(path, "w") as f:
        for s in states:
            f.write(f"{s.i}: begin\n")
            if s.label:
                f.write(f"    // {s.label}\n")
            simple_signal(f, s, "PcNext", "pc_next")
            simple_signal(f, s, "InstLoad", "inst_load")
            simple_signal(f, s, "MemEn", "mem_enable")
            simple_signal(f, s, "MemWr", "mem_write")
            simple_signal(f, s, "MemAddrSel", "mem_addr_sel")
            simple_signal(f, s, "MicroBranch", "microbranch")
            simple_signal(f, s, "RegRead1Sel", "reg_read1_sel")
            simple_signal(f, s, "RegRead2Sel", "reg_read2_sel")
            simple_signal(f, s, "RegWriteSel", "reg_write_sel")
            simple_signal(f, s, "RegOp", "reg_op")
            simple_signal(f, s, "IncOp", "inc_op")
            simple_signal(f, s, "IncReg", "inc_reg")
            simple_signal(f, s, "AluOp", "alu_op")
            simple_signal(f, s, "AluSelA", "alu_sel_a")
            simple_signal(f, s, "AluSelB", "alu_sel_b")
            simple_signal(f, s, "AluFlagSet", "alu_flag_set")
            if s.next_state is not None:
                f.write(f"    next_state = {s.next_state};\n")
            f.write("end\n")

    # Write dispatch load file.
    path = Path(__file__).parent / "cpu_control_dispatch.inc"
    with open(path, "w") as f:
        for s in states:
            if encoding := s.data["Encoding"]:
                f.write(f"8'b{encoding}: state <= {s.i};\n")
