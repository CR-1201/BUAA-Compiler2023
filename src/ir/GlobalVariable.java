package ir;

import backend.mipsComponent.Block;
import backend.mipsInstruction.Move;
import backend.reg.Label;
import backend.reg.Operand;
import ir.constants.*;
import ir.types.PointerType;
import tools.Pair;

import java.util.ArrayList;
import java.util.Arrays;

import static backend.CodeGen.*;

/**
 @author Conroy
 全局简单变量与全局数组
 全局变量可以用常量 Constant 初始化, 不能用变量初始化, 这是语言规范
 本质上 GlobalVariable 和 Function 都属于 GlobalVal 其共同特点是 parent 都是 Module,都是以 @ 开头的
 GlobalVariable 只持有一个 Constant, getValue(0) 就是该常量
 全局变量本质上是一个指针,而不是一个实际的变量
 */
public class GlobalVariable extends user{
    private final boolean isConst;
    /**
     * 初始化
     * 此时全局变量既可以为常量,也可以不是常量
     * @param initVal 是全局变量使用的唯一 value
     */
    public GlobalVariable(String name, Constant initVal, boolean isConst){
        super("@" + name, new PointerType(initVal.getValueType()), module.getModule(),new ArrayList<>()
        {{
            add(initVal);
        }});
        this.isConst = isConst;
    }
    /**
     * @return 初始化 Constant
     */
    public Constant getInitVal(){
        return (Constant) getValue(0);
    }
    @Override
    public String toString(){
        return getName() + " = dso_local " + ((isConst) ? "constant " : "global ") + ((PointerType) getValueType()).getPointeeType() + " " + getValue(0);
    }

    /**
     * 对于全局变量进行分析,并确定全局变量的大小和内容
     * 全局变量有三种:整数,数组,字符串
     * @return mips 全局变量
     */
    public backend.mipsComponent.GlobalVariable buildMipsTree() {
        ArrayList<Integer> elements = new ArrayList<>(); // 用于存放元素的数组
        Constant irInitVal = getInitVal();
        if (irInitVal instanceof ZeroInitializer) { // 没有初始化
            return new backend.mipsComponent.GlobalVariable(getName(), irInitVal.getValueType().getSize());
        } else if (irInitVal instanceof ConstStr) { // 是字符串
            return new backend.mipsComponent.GlobalVariable(getName(), ((ConstStr) irInitVal).getContent());
        } else if (irInitVal instanceof ConstArray) { // 是数组
            ArrayList<ConstInt> dataElements = ((ConstArray) irInitVal).getDataElements();
            for (ConstInt dataElement : dataElements) {
                elements.add(dataElement.getValue());
            }
            return new backend.mipsComponent.GlobalVariable(getName(), elements);
        } else { // 单变量
            elements.add(((ConstInt) irInitVal).getValue());
            return new backend.mipsComponent.GlobalVariable(getName(), elements);
        }
    }
}
