package backend.mipsInstruction;

import backend.reg.Operand;

public class Load extends Instruction{
    private Operand dst;
    private Operand addr;
    private Operand offset;

    public Load(Operand dst, Operand addr, Operand offset) {
        setDst(dst);setAddr(addr);setOffset(offset);
    }
    public void setDst(Operand dst) {
        addDefReg(this.dst, dst);
        this.dst = dst;
    }
    public void setAddr(Operand addr) {
        addUseReg(this.addr, addr);
        this.addr = addr;
    }
    public void setOffset(Operand offset) {
        addUseReg(this.offset, offset);
        this.offset = offset;
    }
    public Operand getAddr() {
        return addr;
    }
    public Operand getOffset() {
        return offset;
    }
    public Operand getDst() {
        return dst;
    }

    @Override
    public void replaceReg(Operand oldReg, Operand newReg) {
        if (dst.equals(oldReg)) setDst(newReg);
        if (addr.equals(oldReg)) setAddr(newReg);
        if (offset.equals(oldReg)) setOffset(newReg);
    }

    @Override
    public void replaceUseReg(Operand oldReg, Operand newReg) {
        if (addr.equals(oldReg)) setAddr(newReg);
        if (offset.equals(oldReg)) setOffset(newReg);
    }

    @Override
    public String toString() {
        return "lw " + dst + ", " + offset + "(" + addr + ")\n";
    }

}
