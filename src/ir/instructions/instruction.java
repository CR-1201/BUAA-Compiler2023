package ir.instructions;

import backend.CodeGen;
import backend.mipsComponent.Block;
import backend.mipsInstruction.Binary;
import backend.mipsInstruction.Shift;
import backend.reg.Operand;
import backend.reg.PhysicsReg;
import ir.BasicBlock;
import ir.Function;
import ir.constants.ConstInt;
import ir.instructions.Binary_Instructions.ICMP;
import ir.instructions.Other_Instructions.PHI;
import ir.types.DataType;
import ir.user;
import ir.value;
import node.FatherNode;
import tools.Pair;

import java.util.ArrayList;

import static backend.CodeGen.blockBlockHashMap;
import static backend.CodeGen.getCodeGen;
import static backend.reg.PhysicsReg.AT;

/**
 @author Conroy
 一切 llvm ir 指令的父类
 会使用 value 作为操作数
 指令的 parent 是 BasicBlock
 */
public abstract class instruction extends user{
    /**
     * @param name     指令名称,不是指 "add" "sub"之类的名称,而是指令返回值存放的虚拟寄存器
     *                 eg: %3 = add i32 %1, %2 名称为 "%3"
     *                 store i32 %1, i32* %2 名称为 "",因为没有用虚拟寄存器
     * @param dataType 指令返回值类型,为 DataType,包括 PointerType,VoidType,IntType
     * @param parent   指令所在基本块
     * @param ops      指令的操作数列表,放在values数组中,从0号位置一次排列. values 数组定义在 user 中
     */
    public instruction(String name, DataType dataType, BasicBlock parent, ArrayList<value> ops) {
        super(name, dataType, parent, ops);
        if (!name.isEmpty()) {
            parent.getParent().addFunctionSymbol(this);
        }
    }
    public void buildMipsTree(BasicBlock block, Function function){
        if (this instanceof PHI || this instanceof ICMP) return;
        System.out.println("The " + this.getClass().toString() + "is not in Conroy-Compiler‘s instructions!");
    }

    public static void mulTemplate(Operand dst, value irOp1, value irOp2, BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
//        mipsBlock.addInstrTail(new Comment(irOp1.getName() + " mul " + irOp2.getName()));
        boolean isOp1Const = irOp1 instanceof ConstInt;
        boolean isOp2Const = irOp2 instanceof ConstInt;
        Operand src1, src2;
        if (isOp1Const || isOp2Const) { // 如果有常数
            int imm;
            if (isOp1Const) {
                src1 = getCodeGen().buildOperand(irOp2, false, irFunction, irBlock);
                imm = ((ConstInt) irOp1).getValue();
            } else {
                src1 = getCodeGen().buildOperand(irOp1, false, irFunction, irBlock);
                imm = ((ConstInt) irOp2).getValue();
            }
            ArrayList<Pair<Boolean, Integer>> mulOptItems = CodeGen.getMulOptItems(imm);
            if (mulOptItems.isEmpty()) { // 如果是空的,那么就说明无法优化
                if (isOp1Const) { // 之所以要跟前面的 src1 分开,是因为如果是可以转化成位移指令,那么就会造成 src2 的冗余解析
                    src2 = getCodeGen().buildOperand(irOp1, false, irFunction, irBlock);
                } else {
                    src2 = getCodeGen().buildOperand(irOp2, false, irFunction, irBlock);
                }
            } else {
                if (mulOptItems.size() == 1) {
                    Shift sll = Shift.Sll(dst, src1, mulOptItems.get(0).getSecond());
                    mipsBlock.addInstrTail(sll);
                    if (!mulOptItems.get(0).getFirst()) {
                        Binary subu = Binary.Subu(dst, PhysicsReg.ZERO, dst);
                        mipsBlock.addInstrTail(subu);
                    }
                } else {
                    Operand at = AT;
                    Shift sll = Shift.Sll(at, src1, mulOptItems.get(0).getSecond()); // 首先用一个 shift
                    mipsBlock.addInstrTail(sll);
                    if (!mulOptItems.get(0).getFirst()) { // 检测要不要负数
                        Binary subu = Binary.Subu(at, PhysicsReg.ZERO, at);
                        mipsBlock.addInstrTail(subu);
                    }
                    for (int i = 1; i < mulOptItems.size() - 1; i++) {  // 开始中间,中间运算的结果存储在 at 中
                        if (mulOptItems.get(i).getSecond() == 0) {
                            mipsBlock.addInstrTail(mulOptItems.get(i).getFirst() ? Binary.Addu(at, at, src1) : Binary.Subu(at, at, src1));
                        } else {
                            Operand tmp = getCodeGen().getTmpReg(irFunction);
                            mipsBlock.addInstrTail(Shift.Sll(tmp, src1, mulOptItems.get(i).getSecond()));
                            mipsBlock.addInstrTail(mulOptItems.get(i).getFirst() ? Binary.Addu(at, at, tmp) : Binary.Subu(at, at, tmp));
                        }
                    }
                    Pair<Boolean, Integer> last = mulOptItems.get(mulOptItems.size() - 1); // 开始结尾
                    if (last.getSecond() == 0) {
                        mipsBlock.addInstrTail(last.getFirst() ? Binary.Addu(dst, at, src1) : Binary.Subu(dst, at, src1));
                    } else {
                        Operand tmp = getCodeGen().getTmpReg(irFunction);
                        mipsBlock.addInstrTail(Shift.Sll(tmp, src1, last.getSecond()));
                        mipsBlock.addInstrTail(last.getFirst() ? Binary.Addu(dst, at, tmp) : Binary.Subu(dst, at, tmp));
                    }
                }
                return;
            }
        } else {
            src1 = getCodeGen().buildOperand(irOp1, false, irFunction, irBlock);
            src2 = getCodeGen().buildOperand(irOp2, false, irFunction, irBlock);
        }
        Binary mul = Binary.Mul(dst, src1, src2);
        mipsBlock.addInstrTail(mul);
    }
    @Override
    public BasicBlock getParent(){
        return (BasicBlock) super.getParent();
    }
    public void eraseFromParent(){
        getParent().removeInstruction(this);
    }
}
