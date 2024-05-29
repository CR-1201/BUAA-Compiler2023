package ir.instructions.Binary_Instructions;

import backend.mipsComponent.Block;
import backend.mipsInstruction.Binary;
import backend.mipsInstruction.Move;
import backend.reg.Immediate;
import backend.reg.Operand;
import ir.BasicBlock;
import ir.constants.ConstInt;
import ir.types.IntType;
import ir.value;

import static backend.CodeGen.blockBlockHashMap;
import static backend.CodeGen.getCodeGen;

/**
 @author Conroy
 <result> = add <ty> <op1>, <op2>
 */
public class ADD extends BinInstruction {
    public ADD(int nameNum, BasicBlock parent, value op1, value op2){
        super(nameNum, new IntType(32), parent, op1, op2);
    }

    /**
     * 可以利用的指令是 addu 和 addiu
     * @param irBlock    当前块
     * @param irFunction 当前函数
     */
    @Override
    public void buildMipsTree(BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        boolean isOp1Const = getOp1() instanceof ConstInt;
        boolean isOp2Const = getOp2() instanceof ConstInt;
        Operand src1, src2;
        Operand dst = getCodeGen().buildOperand(this, false, irFunction, irBlock);
        if (isOp1Const && isOp2Const) { // 全是常数则直接计算
            int op1Imm = ((ConstInt) getOp1()).getValue();
            int op2Imm = ((ConstInt) getOp2()).getValue();
            Move mipsMove= new Move(dst, new Immediate(op1Imm + op2Imm));
            mipsBlock.addInstrTail(mipsMove);
        } else if (isOp1Const) { // 只有 op1 是常数,则交换 op,检验常数是否可以编码的工作,就交给 buildConstInt 了
            src1 = getCodeGen().buildOperand(getOp2(), false, irFunction, irBlock);
            src2 = getCodeGen().buildOperand(getOp1(), true, irFunction, irBlock);
            Binary addu = Binary.Addu(dst, src1, src2);
            mipsBlock.addInstrTail(addu);
        } else {  // 直接加,是不是常数就不管了
            src1 = getCodeGen().buildOperand(getOp1(), false, irFunction, irBlock);
            src2 = getCodeGen().buildOperand(getOp2(), true, irFunction, irBlock);
            Binary addu = Binary.Addu(dst, src1, src2);
            mipsBlock.addInstrTail(addu);
        }
    }
    @Override
    public boolean isCommutative(){
        return true;
    }

    @Override
    public String toString(){
        return getName() + " = add " + getValueType() + " " + getValue(0).getName() + ", " + getValue(1).getName();
    }
}
