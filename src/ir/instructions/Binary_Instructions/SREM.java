package ir.instructions.Binary_Instructions;

import backend.mipsComponent.Block;
import backend.mipsInstruction.Binary;
import backend.mipsInstruction.Move;
import backend.mipsInstruction.Shift;
import backend.reg.Immediate;
import backend.reg.Operand;
import ir.BasicBlock;
import ir.constants.ConstInt;
import ir.types.IntType;
import ir.value;
import tools.Pair;

import static backend.CodeGen.*;
import static backend.reg.PhysicsReg.AT;
import static backend.reg.PhysicsReg.ZERO;

/**
 @author Conroy
 <result> = srem <ty> <op1>, <op2>
 */
public class SREM extends BinInstruction {
    public SREM(int nameNum, BasicBlock parent, value op1, value op2){
        super(nameNum, new IntType(32), parent, op1, op2);
    }
    @Override
    /*
      只有 mod (-)2^l 才会到后端处理, 其余情况在生成 llvm ir 时处理
      @param irBlock    当前块
      @param irFunction 当前函数
     */
    public void buildMipsTree(BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        Operand src1 = getCodeGen().buildOperand(this.getOp1(), false, irFunction, irBlock);
        Operand dst = getCodeGen().buildOperand(this, false, irFunction, irBlock);
        int imm = ((ConstInt) this.getOp2()).getValue();
        int abs = imm > 0 ? imm : -imm;
        if((abs & (abs - 1)) != 0){
            System.out.println("An error has occurred in Srem! " + abs + " is not a power of 2.");
        }
        int l = log2(abs);
        Pair<Operand, Immediate> div = new Pair<>(src1, new Immediate(imm));
        Pair<Block, Pair<Operand, Immediate>> divLookUp = new Pair<>(mipsBlock, div);
        if (divMap.containsKey(divLookUp)) { // 取余可以理解进行完乘法后再进行一个减法
            Operand src2 = divMap.get(divLookUp); // src2 = src1 / imm
            mipsBlock.addInstrTail(Shift.Sll(AT, src2, l)); // src2 << l
            if (imm > 0) { // dst = src1 - src2 << l
                mipsBlock.addInstrTail(Binary.Subu(dst, src1, AT));
            } else {
                mipsBlock.addInstrTail(Binary.Addu(dst, src1, AT));
            }
        } else {
            // 先获得 [31:l] 位的数,然后用原来的数减去这个数
            // 不用 and 的原因是 newDividend 的 [l-1 :0] 与之前的不同,所以没法用了,而且还需要 newDividend 的属性
            Operand dividendHi = getCodeGen().getTmpReg(irFunction);
            if (abs == 1) { // mod 1 || mod - 1
                Move mipsMove = new Move(dst, ZERO);
                mipsBlock.addInstrTail(mipsMove);
            } else {
                Operand newDividend = getCodeGen().buildCeilDividend(src1, l, irBlock, irFunction);
                mipsBlock.addInstrTail(Shift.Srl(AT, newDividend, l)); // dividendHi = {{newDividend[31:l]}, {l{0}}}
                mipsBlock.addInstrTail(Shift.Sll(dividendHi, AT, l));
                mipsBlock.addInstrTail(Binary.Subu(dst, src1, dividendHi));
            }
        }
    }

    @Override
    public boolean isCommutative(){
        return false;
    }

    @Override
    public String toString(){
        return getName() + " = srem " + getValueType() + " " + getValue(0).getName() + ", " + getValue(1).getName();
    }
}
