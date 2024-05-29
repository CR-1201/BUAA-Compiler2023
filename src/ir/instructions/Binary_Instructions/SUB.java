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
 <result> = sub <ty> <op1>, <op2>
 */
public class SUB extends BinInstruction {
    public SUB(int nameNum, BasicBlock parent, value op1, value op2){
        super(nameNum, new IntType(32), parent, op1, op2);
    }
    /**
     * 可以考虑当减数是常量的情况,用 addiu 代替（MARS 无法完成这个）
     * @param irBlock 当前块
     * @param irFunction 当前函数
     */
    @Override
    public void buildMipsTree(BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        boolean isOp1Const = this.getOp1() instanceof ConstInt;
        boolean isOp2Const = this.getOp2() instanceof ConstInt;
        Operand src1, src2;
        Operand dst = getCodeGen().buildOperand(this, false, irFunction, irBlock);
        if (isOp1Const && isOp2Const) { // 如果全是常量，就直接算出来
            int op1Imm = ((ConstInt) this.getOp1()).getValue();
            int op2Imm = ((ConstInt) this.getOp2()).getValue();
            Move mipsMove = new Move(dst, new Immediate(op1Imm - op2Imm));
            mipsBlock.addInstrTail(mipsMove);
        } else if (isOp2Const) { // 如果减数是常量,那么可以用 addi 来代替
            src1 = getCodeGen().buildOperand(this.getOp1(), false, irFunction, irBlock);
            int op2Imm = ((ConstInt) this.getOp2()).getValue();
            src2 = getCodeGen().buildConstIntOperand(-op2Imm, true, irFunction, irBlock);
            Binary mipsAdd = Binary.Addu(dst, src1, src2);
            mipsBlock.addInstrTail(mipsAdd);
        } else {
            src1 = getCodeGen().buildOperand(this.getOp1(), false, irFunction, irBlock);
            src2 = getCodeGen().buildOperand(this.getOp2(), true, irFunction, irBlock);
            Binary sub = Binary.Subu(dst, src1, src2);
            mipsBlock.addInstrTail(sub);
        }
    }
    @Override
    public boolean isCommutative(){
        return false;
    }

    @Override
    public String toString(){
        return getName() + " = sub " + getValueType() + " " + getValue(0).getName() + ", " + getValue(1).getName();
    }
}
