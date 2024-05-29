package ir.instructions.Memory_Instrutions;

import backend.mipsComponent.Block;
import backend.mipsInstruction.Load;
import backend.reg.Operand;
import ir.BasicBlock;
import ir.types.DataType;
import ir.types.PointerType;
import ir.value;

import java.util.ArrayList;
import java.util.Arrays;

import static backend.CodeGen.blockBlockHashMap;
import static backend.CodeGen.getCodeGen;

/**
 @author Conroy
 <result> = load <ty>, <ty>* <pointer>
 */
public class LOAD extends MemInstruction{
    private final DataType dataType; // 从内存中取出来的值类型,只能为 PointerType 或 IntType
    /**
     * @param addr 唯一的操作数,内存地址,其 ValueType 为 PointerType,
     *             只能指向 IntType 或 FPType 或 PointerType(最多双重指针)
     */
    public LOAD(int nameNum, BasicBlock parent, value addr){
        super("%v" + nameNum, (DataType) ((PointerType) addr.getValueType()).getPointeeType(), parent, new ArrayList<>(){{
            add(addr);
        }});
        this.dataType = (DataType) ((PointerType) addr.getValueType()).getPointeeType();
    }

    public value getAddr(){
        return getValue(0);
    }

    public DataType getDataType(){
        return dataType;
    }
    @Override
    public void buildMipsTree( BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        value irAddr = getAddr();
        Operand dst = getCodeGen().buildOperand(this, false, irFunction, irBlock);
        Operand addr = getCodeGen().buildOperand(irAddr, false, irFunction, irBlock); //如果load的地址是二重指针,那么登记后就可以返回了,等价于这个 ir 指令没有对应任何 mips 指令
        Operand offset = getCodeGen().buildConstIntOperand(0, true, irFunction, irBlock);
        Load load = new Load(dst, addr, offset);
        mipsBlock.addInstrTail(load);
    }
    @Override
    public String toString(){
        return getName() + " = load " + getValueType() + ", " + getValue(0).getValueType() + " " + getValue(0).getName();
    }
}
