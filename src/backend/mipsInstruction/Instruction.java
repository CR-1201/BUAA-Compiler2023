package backend.mipsInstruction;

import backend.reg.Operand;
import backend.reg.PhysicsReg;
import backend.reg.Reg;

import java.util.ArrayList;

public class Instruction {
    private final ArrayList<Reg> regDef = new ArrayList<>();
    private final ArrayList<Reg> regUse = new ArrayList<>();
    public Instruction(){}

    /**
     * 之所以要加限制,是因为操作数不一定都是寄存器,只有寄存器会进行登记
     * @param reg 寄存器
     */
    private void addUse(Operand reg) {
        if (reg instanceof Reg) {
            regUse.add((Reg) reg);
        }
    }
    private void addDef(Operand reg) {
        if (reg instanceof Reg) {
            regDef.add((Reg) reg);
        }
    }

    private void removeDef(Operand reg) {
        if (reg instanceof Reg) {
            regDef.remove((Reg) reg);
        }
    }

    private void removeUse(Operand reg) {
        if (reg instanceof Reg) {
            regUse.remove((Reg) reg);
        }
    }

    /**
     * 用于登记该指令使用到的寄存器,如果是目的寄存器,要登记在 def 中; 要是源寄存器,要登记在 use 中
     * @param oldReg 之所以有这个设置,是因为可能一个指令的操作数会被二次修改,这个时候就需要修改其他地方
     * @param newReg 待登记的寄存器
     */
    public void addDefReg(Operand oldReg, Operand newReg) {
        if (oldReg != null) {
            removeDef(oldReg);
        }
        addDef(newReg);
    }

    public void addUseReg(Operand oldReg, Operand newReg) {
        if (oldReg != null) {
            removeUse(oldReg);
        }
        addUse(newReg);
    }
    /**
     * 只有 branch 指令（条件跳转）时候有这个可能为 false
     * @return 当无条件的时候,返回 true
     */
    public boolean noCond() {
        return true;
    }

    /**
     * 表示因此改变的寄存器,可能要比 define 多一些, 这是因为寄存器分配只是分析变量
     */
    public ArrayList<Reg> getWriteRegs() {
        return new ArrayList<>(regDef);
    }

    public ArrayList<Reg> getReadRegs() {
        ArrayList<Reg> readRegs = regUse;
        if (this instanceof Call) {
            readRegs.add(PhysicsReg.SP);
        }
        return readRegs;
    }

    public void replaceReg(Operand oldReg, Operand newReg) {}

    public void replaceUseReg(Operand oldReg, Operand newReg) {}

    public ArrayList<Reg> getRegDef() {
        return regDef;
    }

    public ArrayList<Reg> getRegUse() {
        return regUse;
    }


}
