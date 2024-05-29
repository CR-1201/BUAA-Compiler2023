package ir.instructions.Memory_Instrutions;

import backend.mipsComponent.Block;
import backend.mipsComponent.Function;
import backend.mipsInstruction.Binary;
import backend.reg.Operand;
import ir.BasicBlock;
import ir.constants.ConstArray;
import ir.types.PointerType;
import ir.types.valueType;
import ir.user;

import java.util.ArrayList;
import java.util.Arrays;

import static backend.CodeGen.*;
import static backend.reg.PhysicsReg.SP;

public class ALLOCA extends MemInstruction{
    /*
     * 参考 GlobalVariable 的设计,主要是为了局部常量数组准备的, 记录着局部常量数组的值
     * TODO 如果一个常量数组只被当成常量使用,也就是没有 const[var] 这种状况
     * 可以在后续的优化中将所有的 store 删掉
     */
    private final ConstArray initVal;
    /**
     * 新建一个 alloca 指令,其类型是分配空间类型的指针
     * @param nameNum 对于指令而言,其名称中带有数字,指令名称中的数字,eg: 名称为 %1 的指令的 nameNum 为 1
     * @param allocatedType 分配空间的类型,可能为 PointerType, IntType, ArrayType
     * @param parent 基本块
     */
    public ALLOCA(int nameNum, valueType allocatedType, BasicBlock parent){
        super("%v" + nameNum, new PointerType(allocatedType), parent,new ArrayList<>()); // 指针  没有初始化
        this.initVal = null;
    }

    public ALLOCA(int nameNum, valueType allocatedType, BasicBlock parent, ConstArray initVal){
        // 指针
        super("%v" + nameNum, new PointerType(allocatedType), parent,new ArrayList<>()); // 指针 有初始化
        this.initVal = initVal;
    }

    public valueType getAllocatedType(){
        PointerType pointer =  (PointerType) getValueType();
        return pointer.getPointeeType();
    }

    public ConstArray getInitVal(){
        return initVal;
    }

    /**
     * 可以被提升,本质是只要是没有使用 gep 的,都可以被提升
     * 直观理解,就是和数组不挂钩的,都可以在 mem2reg 中被提升
     * @return 可提升,则为 true
     */
    public boolean Promotable(){
        if (getUsers().isEmpty()){ // 没有使用
            return true;
        }
        for (user user : getUsers()){ // 使用者中有 GEP ,则与数组有关,否则一般使用者都是 load,store 之类的
            if (user instanceof GEP gep){
                if (gep.getValue(0) == this){ // promotable alloca must be a single data
                    return false;
                }
            }
        }
        return true;
    }
    @Override
    public void buildMipsTree(BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        Function mipsFunction = functionHashMap.get(irFunction);
        valueType pointeeType = ((PointerType) getValueType()).getPointeeType(); // 获得指针指向的类型
//        mipsBlock.addInstrTail(new Comment("Alloca from the offset: " + mipsFunction.getAllocaSize() + ", size is: " + pointeeType.getSize()));
        Operand offset = getCodeGen().buildConstIntOperand(mipsFunction.getAllocaSize(),true, irFunction, irBlock); // alloc 前在栈上已分配出的空间
        mipsFunction.addAllocaSize(pointeeType.getSize());
        // 这里进行的是栈的恢复操作,是因为栈会在 mips 函数一开始就生长出所有 alloc 的空间
        // 只需要将 alloc 的空间像 heap 一样使用
        Operand dst = getCodeGen().buildOperand(this, true, irFunction, irBlock);
        Binary add = Binary.Addu(dst, SP, offset);
        mipsBlock.addInstrTail(add);
    }

    @Override
    public String toString(){
        return getName() + " = alloca " + getAllocatedType();
    }
}
