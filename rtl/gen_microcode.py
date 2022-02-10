import csv
from pathlib import Path
import math

BINARY = {"Yes": "1", "No": "0"}
REG_SELECT = {
    "A": "RegSelA",
    "Reg8Src": "RegSelReg8Src",
    "Reg8Dest": "RegSelReg8Dest",
}
SIGNALS = {
    "Label": None,
    "Encoding": None,
    "NextState": None,
    "PcNext": {
        "Same": "PcNextSame",
        "Inc": "PcNextInc",
    },
    "RegRead1Sel": REG_SELECT,
    "RegRead2Sel": REG_SELECT,
    "RegWriteSel": REG_SELECT,
    "RegWriteInput": {
        "Alu": "RegInputAlu",
        "Mem": "RegInputMem",
    },
    "RegWriteEn": BINARY,
    "AluOp": {
        "CopyA": "AluOpCopyA",
        "IncA": "AluOpIncA",
    },
    "AluSelA": {
        "Reg1": "AluSelAReg1",
    },
    "AluSelB": {
        "Reg2": "AluSelBReg2",
    },
    "MemEn": BINARY,
    "MemWr": BINARY,
    "MicroBranch": {
        "Next": "MicroBranchNext",
        "Jump": "MicroBranchJump",
        "Cond": "MicroBranchCond",
        "Fetch": "MicroBranchDispatch",
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


        if self.data["MicroBranch"] == "Fetch":
            forced = {
                "PcNext": "Inc",
                "MemEn": "Yes",
                "MemWr": "No",
                "NextState": "-",
                "InstLoad": "Yes",
            }
            for k, v in forced.items():
                assert self.data.get(k, "-") == "-"
                self.data[k] = v
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
            simple_signal(f, s, "MicroBranch", "microbranch")
            simple_signal(f, s, "RegRead1Sel", "reg_read1_sel")
            simple_signal(f, s, "RegRead2Sel", "reg_read2_sel")
            simple_signal(f, s, "RegWriteSel", "reg_write_sel")
            simple_signal(f, s, "RegWriteEn", "reg_write_enable")
            simple_signal(f, s, "RegWriteInput", "reg_write_input")
            simple_signal(f, s, "AluOp", "alu_op")
            simple_signal(f, s, "AluSelA", "alu_sel_a")
            simple_signal(f, s, "AluSelB", "alu_sel_b")
            if s.next_state is not None:
                f.write(f"    next_state = {s.next_state};\n")
            f.write("end\n")

    # Write dispatch load file.
    path = Path(__file__).parent / "cpu_control_dispatch.inc"
    with open(path, "w") as f:
        for s in states:
            if encoding := s.data["Encoding"]:
                f.write(f"8'b{encoding}: state <= {s.i};\n")

