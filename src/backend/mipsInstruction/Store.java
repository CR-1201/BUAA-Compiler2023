package backend.mipsInstruction;


import backend.reg.Operand;

public class Store extends Instruction {
    private Operand src;
    private Operand addr;
    private Operand offset;
    public Store(Operand src, Operand addr, Operand offset) {
        setSrc(src);setAddr(addr);setOffset(offset);
    }
    public void setSrc(Operand src) {
        addUseReg(this.src, src);
        this.src = src;
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
    public Operand getSrc() {
        return src;
    }
    @Override
    public void replaceReg(Operand oldReg, Operand newReg) {
        if (src.equals(oldReg)) setSrc(newReg);
        if (offset.equals(oldReg)) setOffset(newReg);
        if (addr.equals(oldReg)) setAddr(newReg);
    }

    @Override
    public void replaceUseReg(Operand oldReg, Operand newReg) {
        if (src.equals(oldReg)) setSrc(newReg);
        if (offset.equals(oldReg)) setOffset(newReg);
        if (addr.equals(oldReg)) setAddr(newReg);
    }

    @Override
    public String toString() {
        return "sw " + src + ", " + offset + "(" + addr + ")\n";
    }
}
