package ir.instructions.Binary_Instructions;

import backend.mipsComponent.Block;
import backend.mipsInstruction.Binary;
import backend.mipsInstruction.Shift;
import backend.reg.Operand;
import backend.reg.PhysicsReg;
import ir.BasicBlock;
import ir.constants.ConstInt;
import ir.types.IntType;
import ir.value;
import tools.Pair;

import java.util.ArrayList;

import static backend.CodeGen.*;
import static backend.reg.PhysicsReg.AT;

/**
 @author Conroy
 <result> = mul <ty> <op1>, <op2>
 */
public class MUL extends BinInstruction{
    public MUL(int nameNum, BasicBlock parent, value op1, value op2){
        super(nameNum, new IntType(32), parent, op1, op2);
    }
    @Override
    /*
     * 将一个乘常数分解成了多一个 (+-shift) 的项
     * @param instr      乘法指令
     * @param irBlock    当前块
     * @param irFunction 当前函数
     */
    public void buildMipsTree( BasicBlock irBlock, ir.Function irFunction) {
        Operand dst = getCodeGen().buildOperand(this, false, irFunction, irBlock);
        mulTemplate(dst, this.getOp1(), this.getOp2(), irBlock, irFunction);
    }

    @Override
    public boolean isCommutative(){
        return true;
    }
    @Override
    public String toString() {
        return getName() + " = mul " + getValueType() + " " + getValue(0).getName() + ", " + getValue(1).getName();
    }
}