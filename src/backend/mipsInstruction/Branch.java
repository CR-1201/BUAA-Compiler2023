package backend.mipsInstruction;

import backend.mipsComponent.Block;
import backend.reg.Operand;

import static backend.mipsInstruction.CondType.*;


public class Branch extends Instruction{
    private CondType cond;
    private Operand op1 = null;
    private Operand op2 = null;
    private final int opNum;
    private Block target;

    /**
     * 无条件跳转 j target
     * @param target 目标块
     */
    public Branch(Block target) {
        this.target = target;
        this.cond = ANY;
        this.opNum = 0;
    }

    public Branch(CondType cond, Operand op1, Operand op2, Block target) {
        this.cond = cond;
        setOp1(op1);
        setOp2(op2);
        this.target = target;
        this.opNum = 2;
    }

    public void setTarget(Block target) {
        this.target = target;
    }

    public Block getTarget() {
        return target;
    }

    public CondType getCond() {
        return cond;
    }

    public void setCond(CondType cond) {
        this.cond = cond;
    }

    public void setOp1(Operand op1) {
        addUseReg(this.op1, op1);
        this.op1 = op1;
    }

    public void setOp2(Operand op2) {
        addUseReg(this.op2, op2);
        this.op2 = op2;
    }

    public boolean noCond() {
        return cond.equals(ANY);
    }

    @Override
    public void replaceReg(Operand oldReg, Operand newReg) {
        if (op1.equals(oldReg)) setOp1(newReg);
        if (op2.equals(oldReg)) setOp2(newReg);
    }

    @Override
    public void replaceUseReg(Operand oldReg, Operand newReg) {
        if (op1.equals(oldReg)) setOp1(newReg);
        if (op2.equals(oldReg)) setOp2(newReg);
    }

    @Override
    public String toString() {
        return switch (opNum) {
            case 0 -> "j " + target.getName() + "\n";
            case 1 -> "b" + cond + "z " + target.getName() + "\n";
            case 2 -> "b" + cond + " " + op1 + ", " + op2 + ", " + target.getName() + "\n";
            default -> "Branch wrong! opNum = " + opNum + "\n";
        };
    }

}
