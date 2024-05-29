package ir.instructions.Binary_Instructions;

import backend.mipsComponent.Block;
import backend.mipsInstruction.Binary;
import backend.mipsInstruction.HiMove;
import backend.mipsInstruction.Move;
import backend.mipsInstruction.Shift;
import backend.reg.Immediate;
import backend.reg.Operand;
import backend.reg.PhysicsReg;
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
 <result> = sdiv <ty> <op1>, <op2>
 */
public class SDIV extends BinInstruction{
    public SDIV(int nameNum, BasicBlock parent, value op1, value op2){
        super(nameNum, new IntType(32), parent, op1, op2);
    }

    @Override
    public void buildMipsTree( BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
//        mipsBlock.addInstrTail(new Comment(instr.getOp1().getName() + " div " + instr.getOp2().getName()));
        Operand src1 = getCodeGen().buildOperand(getOp1(), false, irFunction, irBlock); // 除法中不允许出现立即数
        boolean isSrc2Const = getOp2() instanceof ConstInt; // 如果除数是常数,就可以进行除常数优化了
        if (isSrc2Const) {
            int immediate = ((ConstInt) getOp2()).getValue(); // 获得除数常量
            if (immediate == 1) { // 如果除数是 1 ,将 ir 映射成被除数
                operandMap.put(this, src1);
            } else {
                Pair<Operand, Operand> div = new Pair<>(src1, new Immediate(immediate));
                Pair<Block, Pair<Operand, Operand>> divLookUp = new Pair<>(mipsBlock, div);
                if ( divMap.containsKey(divLookUp)) {
                    operandMap.put(this, divMap.get(divLookUp)); // 后续用到则直接查询即可
                } else {
                    Operand dst = getCodeGen().buildOperand(this, false, irFunction, irBlock);
                    constDiv(dst, src1, immediate, irBlock, irFunction); // 只能进行运算
                }
            }
        } else { // 无法常数优化
            Operand src2 = getCodeGen().buildOperand(this.getOp2(), false, irFunction, irBlock);
            Operand dst = getCodeGen().buildOperand(this, false, irFunction, irBlock);
            Binary mipsDiv = Binary.Div(dst, src1, src2);
            mipsBlock.addInstrTail(mipsDiv);
        }
    }
    private void constDiv(Operand dst, Operand dividend, int divImm, BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        int abs = divImm > 0 ? divImm : -divImm; // 这里之所以取 abs,因为在之后如果是负数,会有一个取相反数的操作
        if (divImm == -1) {  // 如果除数是 -1,那么就是取相反数
            Binary rsb = Binary.Subu(dst, PhysicsReg.ZERO, dividend); // dst = 0 - dividend
            mipsBlock.addInstrTail(rsb);
            return;
        } else if (divImm == 1) { // 如果除数是 1,直接 move
            Move mipsMove = new Move(dst, dividend);
            mipsBlock.addInstrTail(mipsMove);
        } else if ((abs & (abs - 1)) == 0) {  // 如果是 2 的幂次
            int l = log2(abs); // l = log2(abs)
            Operand newDividend = getCodeGen().buildCeilDividend(dividend, l, irBlock, irFunction); // 产生新的被除数
            Shift mipsShift = Shift.Sra(dst, newDividend, l); // 将被除数右移
            mipsBlock.addInstrTail(mipsShift);
        } else { // dst = dividend / abs => dst = (dividend * n) >> shift
            long nc = ((long) 1 << 31) - (((long) 1 << 31) % abs) - 1; // nc = 2^31 - 2^31 % abs - 1
            long p = 32;
            while (((long) 1 << p) <= nc * (abs - ((long) 1 << p) % abs)) { // 2^p > (2^31 - 2^31 % abs - 1) * (abs - 2^p % abs)
                p++;
            }
            // m = (2^p + abs - 2^p % abs) / abs
            long m = ((((long) 1 << p) + (long) abs - ((long) 1 << p) % abs) / (long) abs); // m 是 2^p / abs 的向上取整
            int n = (int) ((m << 32) >>> 32); // >>> 是无符号右移的意思,所以 n = m[31:0]
            int shift = (int) (p - 32);
            Operand tmp0 = getCodeGen().getTmpReg(irFunction); // tmp0 = n
            Move mipsMove = new Move(tmp0, new Immediate(n));
            mipsBlock.addInstrTail(mipsMove);
            Operand tmp1 = getCodeGen().getTmpReg(irFunction);
            if (m >= 0x80000000L) { // tmp1 = dividend + (dividend * n)[63:32]
                HiMove mipsMthi = HiMove.Mthi(dividend);
                mipsBlock.addInstrTail(mipsMthi);
                Binary smMadd = Binary.SmMadd(tmp1, dividend, tmp0); // 这里的 madd 要求是有符号的,具体为啥我也不知道
                mipsBlock.addInstrTail(smMadd);
            }
            else { // tmp1 = (dividend * n)[63:32]
                Binary smmul = Binary.SmMul(tmp1, dividend, tmp0); // 但是这里的 smmul 则是有符号的
                mipsBlock.addInstrTail(smmul);
            }
            Operand tmp2 = getCodeGen().getTmpReg(irFunction);
            Shift instr_sra = Shift.Sra(tmp2, tmp1, shift); // tmp2 = tmp1 >> shift
            mipsBlock.addInstrTail(instr_sra);
            Shift srl = Shift.Srl(AT, dividend, 31); // dst = tmp2 + dividend >> 31
            mipsBlock.addInstrTail(srl);
            Binary instr_addu = Binary.Addu(dst, tmp2, AT);
            mipsBlock.addInstrTail(instr_addu);
        }
        if (divImm < 0) { // 这里依然是进行了一个取相反数的操作
            Binary rsb = Binary.Subu(dst, ZERO, dst);
            mipsBlock.addInstrTail(rsb);
        }
        divMap.put(new Pair<>(mipsBlock, new Pair<>(dividend, new Immediate(divImm))), dst);
    }
    @Override
    public boolean isCommutative(){
        return false;
    }

    @Override
    public String toString(){
        return getName() + " = sdiv " + getValueType() + " " + getValue(0).getName() + ", " + getValue(1).getName();
    }
}
