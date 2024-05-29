package backend.mipsInstruction;

import backend.reg.Operand;

public class HiMove extends Instruction{
    public static HiMove Mthi(Operand op) {
        return new HiMove("mthi", null, op);
    }
    public static HiMove Mfhi(Operand dst) {
        return new HiMove("mfhi", dst, null);
    }
    private final String type;
    private Operand dst;
    private Operand op;
    public HiMove(String type, Operand dst, Operand op) {
        this.type = type;setDst(dst);setOp(op);
    }
    public void setDst(Operand dst) {
        if (dst != null) {
            addDefReg(this.dst, dst);
        }
        this.dst = dst;
    }
    public void setOp(Operand op) {
        if (op != null) {
            addUseReg(this.op, op);
        }
        this.op = op;
    }
    public Operand getDst() {
        return dst;
    }
    public Operand getOp() {
        return op;
    }
    @Override
    public void replaceReg(Operand oldReg, Operand newReg) {
        if (dst != null) {
            if (dst.equals(oldReg)) setDst(newReg);
        }
        if (op != null) {
            if (op.equals(oldReg)) setOp(newReg);
        }
    }
    @Override
    public void replaceUseReg(Operand oldReg, Operand newReg) {
        if (op != null) {
            if (op.equals(oldReg)) setOp(newReg);
        }
    }
    @Override
    public String toString() {
        switch (type){
            case "mthi":
                return "mthi " + op + "\n";
            case "mfhi":
                return "mfhi " + dst + "\n";
            default:
                assert false: "wrong hiMove\n";
                return "";
        }
    }

}
