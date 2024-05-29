package ir.instructions.Binary_Instructions;

import ir.BasicBlock;
import ir.instructions.instruction;
import ir.types.DataType;
import ir.value;

import java.util.ArrayList;
import java.util.Arrays;

/**
 @author Conroy
 有左右2个操作数,分别放在 values 列表的 0 号与 1 号位
 操作数可以为常数,也可以为指令
 */
public abstract class BinInstruction extends instruction {
    /**
     * @param nameNum 指令名称中的数字,eg: 名称为 %1 的指令的 nameNum 为 1
     * @param op1     第一个操作数
     * @param op2     第二个操作数
     */
    BinInstruction(int nameNum, DataType dataType, BasicBlock parent, value op1, value op2) {
        super("%v" + nameNum, dataType, parent, new ArrayList<>(){{
            add(op1);add(op2);
        }});
    }
    public value getOp1(){
        return getValue(0);
    }
    public value getOp2(){
        return getValue(1);
    }
    public abstract boolean isCommutative(); //操作数 op1 和op2 是不是可交换的
    /**
     * 两个指令是否是相同的运算符
     * @param instr1 指令 1
     * @param instr2 指令 2
     * @return 是则为 true
     */
    public static boolean isSameOp(BinInstruction instr1, BinInstruction instr2){
        boolean isSame = false;
        if (instr1 instanceof ADD && instr2 instanceof ADD) isSame = true;
        else if (instr1 instanceof SUB && instr2 instanceof SUB) isSame = true;
        else if (instr1 instanceof MUL && instr2 instanceof MUL) isSame = true;
        else if (instr1 instanceof SDIV && instr2 instanceof SDIV) isSame = true;
        else if (instr1 instanceof SREM && instr2 instanceof SREM) isSame = true;
        else if (instr1 instanceof ICMP cmp_1 && instr2 instanceof ICMP cmp_2){
            isSame =  cmp_1.getCondition().equals(cmp_2.getCondition());
        }
        return isSame;
    }
}
