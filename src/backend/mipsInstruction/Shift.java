package backend.mipsInstruction;

import backend.reg.Operand;

import static backend.mipsInstruction.Shift.ShiftType.*;

public class Shift extends Instruction{
    public enum ShiftType {  // arithmetic right  |  logic left  |  logic right
        SRA, SLL, SRL
    }
    public static Shift Sll(Operand dst, Operand src, int shift) {
        return new Shift(SLL, dst, src, shift);
    }
    public static Shift Sra(Operand dst, Operand src, int shift) {
        return new Shift(SRA, dst, src, shift);
    }
    public static Shift Srl(Operand dst, Operand src, int shift) {
        return new Shift(SRL, dst, src, shift);
    }
    private Operand dst;
    private Operand src;
    private final ShiftType type;
    private final int shift;

    public Shift(ShiftType type, Operand dst, Operand src, int shift) {
        this.type = type;this.shift = shift;
        setDst(dst);setSrc(src);
    }
    public void setDst(Operand dst) {
        addDefReg(this.dst, dst);
        this.dst = dst;
    }
    public void setSrc(Operand src) {
        addUseReg(this.src, src);
        this.src = src;
    }

    @Override
    public void replaceReg(Operand oldReg, Operand newReg) {
        if (dst.equals(oldReg)) setDst(newReg);
        if (src.equals(oldReg)) setSrc(newReg);
    }
    @Override
    public void replaceUseReg(Operand oldReg, Operand newReg) {
        if (src.equals(oldReg)) setSrc(newReg);
    }

    @Override
    public String toString() {
        return switch (type) {
            case SRA -> "sra " + dst + ", " + src + ", " + shift + "\n";
            case SRL -> "srl " + dst + ", " + src + ", " + shift + "\n";
            default -> "sll " + dst + ", " + src + ", " + shift + "\n";
        };
    }

}
