package ir.instructions.Memory_Instrutions;

import backend.mipsComponent.Block;
import backend.mipsInstruction.Binary;
import backend.reg.Operand;
import ir.BasicBlock;
import ir.constants.ConstInt;
import ir.types.ArrayType;
import ir.types.PointerType;
import ir.types.valueType;
import ir.value;

import java.util.ArrayList;
import java.util.Arrays;

import static backend.CodeGen.*;
import static backend.reg.PhysicsReg.AT;

/**
 @author Conroy
 规定 getelemtnptr 只寻址一次,因此只有2个操作数——基址与下标,分别存放在 values 数组的 0,1号位
 例如:
 int c[10][20][30][40];
 c[1][1][1][1];
 %10 = getelementptr inbounds [10 x [20 x [30 x [40 x i32]]]], [10 x [20 x [30 x [40 x i32]]]]* %4, i64 0, i64 1
 %11 = getelementptr inbounds [20 x [30 x [40 x i32]]], [20 x [30 x [40 x i32]]]* %10, i64 0, i64 1
 %12 = getelementptr inbounds [30 x [40 x i32]], [30 x [40 x i32]]* %11, i64 0, i64 1
 %13 = getelementptr inbounds [40 x i32], [40 x i32]* %12, i64 0, i64 1
 %14 = load i32, i32* %13, align 4
 可见llvm ir官方用了四条GEP指令寻址四维数组
 inbounds 表示执行越界检查,从界外掷入界内的,不重要
 对于 getelementptr [4 x i32] [4 x i32]* base, i32 A, i32 B
 A 是第一个 index 它的计算是 sizeof([4 x i32]) * A
 B 是第二个 index 它的计算是 sizeof(i32) * B
 一般 B 比较常用,而 A 一般为 0
 无论是有 A, B 两个 index,还是只有 A 一个 index,上面的算法都成立
 但是 index 的个数不同,GEP 的类型不同
 当没有 B 时,因为不涉及定位到 i32,因此 getelementptr 的返回值类型为 [4 x i32]*
 当有 B 时,返回值为 i32*
 在代码里，有
 getelementptr baseType baseType* base, A, B;     ret: baseType.getElementType*
 getelementptr baseType baseType* base, A;        ret: baseType*
 */
public class GEP extends MemInstruction{
    private final valueType baseType;
    /**
     * 只有一个下标,用于 (函数传参int a[][2]),然后 a[2][1]型寻址
     * @param base       第一个操作数,基址（其类型是一个指针）
     * @param firstIndex 第二个操作数,指针寻址下标
     */
    public GEP(int nameNum, BasicBlock parent, value base, value firstIndex){
        super("%v" + nameNum, (PointerType) base.getValueType(), parent,new ArrayList<>(){{
            add(base);add(firstIndex);
        }});
        this.baseType = ((PointerType) base.getValueType()).getPointeeType();
    }

    /**
     * 有两个下标,用于int a[2][3];a[1][2]型正常寻址
     * @param base        第一个操作数,基址（其类型是一个指针）
     * @param firstIndex  第二个操作数,指针寻址下标（通常是 0）
     * @param secondIndex 第三个操作数,数组寻址下标
     */
    public GEP(int nameNum, BasicBlock parent, value base, value firstIndex, value secondIndex){
        super("%v" + nameNum, new PointerType(((ArrayType) ((PointerType) base.getValueType()).getPointeeType()).getElementType()),
                parent, new ArrayList<>(){{
                    add(base);add(firstIndex);add(secondIndex);
                }});
        this.baseType = ((PointerType) base.getValueType()).getPointeeType();
    }

    public value getBase(){
        return getValue(0);
    }

    public valueType getBaseType(){
        return baseType;
    }

    /**
     * @return 下标列表,包含1个或2个下标
     */
    public ArrayList<value> getIndex(){
        ArrayList<value> result = new ArrayList<>();
        for (int i = 1; i < getNumOfOps(); i++){
            result.add(getValue(i));
        }
        return result;
    }
    /**
     * 用于解析然后生成指向特定元素的一个指针
     */
    @Override
    public void buildMipsTree( BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        Operand base = getCodeGen().buildOperand(getBase(), false, irFunction, irBlock);// 获得数组的基地址
//        mipsBlock.addInstrTail(new Comment("GEP base: " + instr.getBase().getName()));
        Operand dst = getCodeGen().buildOperand(this, false, irFunction, irBlock);
        if (getNumOfOps() == 2) { // 说明此时是一个指向 int 的一维指针
            valueType baseType = getBaseType();
            value irIndex = getIndex().get(0);
            if (irIndex instanceof ConstInt) { // 对于常数,是可以进行优化,直接用一个 add 算出来
                int totalIndex = baseType.getSize() * ((ConstInt) irIndex).getValue();
                if (totalIndex != 0) {
                    Operand mipsTotalIndex = getCodeGen().buildConstIntOperand(totalIndex, true, irFunction, irBlock);
                    Binary addu = Binary.Addu(dst, base, mipsTotalIndex);
                    mipsBlock.addInstrTail(addu);
                } else {
                    operandMap.put(this, base);
                }
            } else { // 如果是变量,那么就需要用 mla
                mulTemplate(dst, irIndex, new ConstInt(baseType.getSize()), irBlock, irFunction);
                Binary addu = Binary.Addu(dst, dst, base);
                mipsBlock.addInstrTail(addu);
            }
        } else if (getNumOfOps() == 3) { // 指向一个数组
            ArrayType baseType = (ArrayType) getBaseType();// 获得指针指向的类型,应该是一个数组类型
            valueType elementType = baseType.getElementType(); // 获得数组元素类型
            value irIndex0 = getIndex().get(0);
            value irIndex1 = getIndex().get(1);
//            mipsBlock.addInstrTail(new Comment("the first index"));
            // 首先看 A 偏移,有 base += totalIndex0 (IndexA * baseSize)
            if (irIndex0 instanceof ConstInt) {
                int totalIndex0 = baseType.getSize() * ((ConstInt) irIndex0).getValue();
                Operand mipsTotalIndex0 = getCodeGen().buildConstIntOperand(totalIndex0, true, irFunction, irBlock);
                Binary addu = Binary.Addu(dst, base, mipsTotalIndex0);
                mipsBlock.addInstrTail(addu);
            } else {// 此时 dst 为 总偏移量 0
                mulTemplate(dst, irIndex0, new ConstInt(baseType.getSize()), irBlock, irFunction);
                Binary addu = Binary.Addu(dst, dst, base);  // 此时 dst = base + mipsStep0 * mipsIndex0
                mipsBlock.addInstrTail(addu);
            }
//            mipsBlock.addInstrTail(new Comment("the second index"));
            if (irIndex1 instanceof ConstInt) { // 然后看 B 偏移
                int totalIndex1 = elementType.getSize() * ((ConstInt) irIndex1).getValue();
                Operand mipsTotalIndex1 = getCodeGen().buildConstIntOperand(totalIndex1, true, irFunction, irBlock);
                Binary addu = Binary.Addu(dst, dst, mipsTotalIndex1);
                mipsBlock.addInstrTail(addu);
            } else {
                Operand totalIndex1 = AT; // 先算 totalIndex1 = mipsStep1 * mipsIndex1
                mulTemplate(totalIndex1, irIndex1, new ConstInt(elementType.getSize()), irBlock, irFunction);
                Binary addu = Binary.Addu(dst, totalIndex1, dst); // 然后算 dst += totalIndex1
                mipsBlock.addInstrTail(addu);
            }
        }
    }
    @Override
    public String toString(){
        StringBuilder s = new StringBuilder(getName() + " = getelementptr inbounds " + baseType + ", ");
        int n = getNumOfOps();
        for (int i = 0; i < n; i++){
            s.append(getValue(i).getValueType()).append(" ").append(getValue(i).getName());
            if( i+1 < n )s.append(", ");
        }
//        s.delete(s.length() - 2, s.length());
        return s.toString();
    }
}
