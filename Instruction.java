public class Instruction {
    public String type;
    public int immediate, address;
    public String opcode, rs, rt, rd;

    public Instruction(String type, String opcode, String rs, String rt, String rd) {
        this.opcode = opcode;
        this.type = type;
        this.rs = rs;
        this.rt = rt;
        this.rd = rd;
    }

    public Instruction(String type, String opcode, String rs, String rt, int immediate) {
        this.opcode = opcode;
        this.type = type;
        this.rs = rs;
        this.rt = rt;
        this.immediate = immediate;
    }

    public Instruction(String type, String opcode, int address) {
        this.type = type;
        this.opcode = opcode;
        this.address = address;
    }

    public Instruction(String type, String opcode) {
        this.type = type;
        this.opcode = opcode;
    }

}
