package ir.instructions.Other_Instructions;

import ir.BasicBlock;
import ir.instructions.Binary_Instructions.ICMP;
import ir.instructions.instruction;
import ir.types.IntType;
import ir.value;

import java.util.ArrayList;

import static backend.CodeGen.getCodeGen;
import static backend.CodeGen.operandMap;

/**
 @author Conroy
 <result> = zext <ty> <value> to <ty2>
 %7 = zext i1 %6 to i32
 */
public class ZEXT extends instruction {
    /**
     * @param parent 基本块
     * @param value  被转变的值
     */
    public ZEXT(int nameNum, BasicBlock parent, value value){
        super("%v" + nameNum, new IntType(32), parent, new ArrayList<>(){{
            add(value);
        }});
    }
    public value getConversionValue(){
        return getValue(0);
    }
    /**
     * 只需要将这条指令与 Icmp 的 dst 对应起来即可,甚至连 move 都不需要
     * @param irBlock 当前块
     * @param irFunction 当前函数
     */
    @Override
    public void buildMipsTree( BasicBlock irBlock, ir.Function irFunction) {
        getCodeGen().buildIcmp((ICMP) getConversionValue(), irBlock, irFunction);
        operandMap.put(this, operandMap.get(getConversionValue()));
    }
    @Override
    public String toString(){
        return this.getName() + " = zext i1 " + getValue(0).getName() + " to i32";
    }
}
