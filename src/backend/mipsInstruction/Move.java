package backend.mipsInstruction;

import backend.reg.Immediate;
import backend.reg.Label;
import backend.reg.Operand;

public class Move extends Instruction{
    private Operand dst;
    private Operand src;

    public Move(Operand dst, Operand src) {
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

    public Operand getDst() {
        return dst;
    }

    public Operand getSrc() {
        return src;
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
        if (src instanceof Immediate) { // 如果是一个立即数,那么就用 li
            return "li " + dst + ", " + src + "\n";
        } else if (src instanceof Label) {
            return "la " + dst + ", " + src + "\n";
        } else {
            if(dst.equals(src)){
                return "";
            } else return "move " + dst + ", " + src + "\n";
        }
    }
}
