package ir.instructions.Memory_Instrutions;

import backend.mipsComponent.Block;
import backend.mipsInstruction.Store;
import backend.reg.Operand;
import ir.BasicBlock;
import ir.types.VoidType;
import ir.value;

import java.util.ArrayList;
import java.util.Arrays;

import static backend.CodeGen.blockBlockHashMap;
import static backend.CodeGen.getCodeGen;

/**
 @author Conroy
 store <ty> <value>, <ty>* <pointer>
 */
public class STORE extends MemInstruction{
    /**
     * 因为 Store 没有返回值,所以连名字也不配拥有
     * @param value     第一个操作数,写入内存的值,valueType 为 IntType
     * @param addr      第二个操作数,写入内存的地址,valueType 为 PointerType,只能指向 IntType 或 PointerType（最多双重指针）
     */
    public STORE(BasicBlock parent, value value, value addr){
        super("", new VoidType(), parent, new ArrayList<>(){{
            add(value);
            add(addr);
        }});
    }
    public value getValue(){
        return getValue(0);
    }
    public value getAddr(){
        return getValue(1);
    }
    @Override
    public void buildMipsTree(BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        value irAddr = getAddr();
        Operand src = getCodeGen().buildOperand(getValue(), false, irFunction, irBlock);
        Operand addr = getCodeGen().buildOperand(irAddr, false, irFunction, irBlock);
        Operand offset = getCodeGen().buildConstIntOperand(0, true, irFunction, irBlock);
        Store store = new Store(src, addr, offset);
        mipsBlock.addInstrTail(store);
    }

    @Override
    public String toString(){
        return "store " + getValue(0).getValueType() + " " + getValue(0).getName() + ", " +
                getValue(1).getValueType() + " " + getValue(1).getName();
    }
}
