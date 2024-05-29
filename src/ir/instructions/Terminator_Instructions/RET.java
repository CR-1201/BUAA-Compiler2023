package ir.instructions.Terminator_Instructions;

import backend.mipsComponent.Block;
import backend.mipsComponent.Function;
import backend.mipsInstruction.Move;
import backend.mipsInstruction.Ret;
import backend.reg.Operand;
import ir.BasicBlock;
import ir.types.DataType;
import ir.types.VoidType;
import ir.value;

import java.util.ArrayList;
import java.util.Arrays;

import static backend.CodeGen.*;
import static backend.reg.PhysicsReg.V0;

/**
 @author Conroy
 ret <type> <value> ,ret void
 */
public class RET extends TerInstruction{
    /**
     * 返回值为void
     */
    public RET(BasicBlock parent){
        super(new VoidType(), parent, new ArrayList<>());
    }
    /**
     * 返回值不为void
     * @param retValue 返回值,ValueType 为 FPType 或 IntType
     */
    public RET(BasicBlock parent, value retValue){
        super((DataType) retValue.getValueType(), parent, new ArrayList<>(){{
            add(retValue);
        }});
    }

    public value getRetValue(){
        if (getValueType() instanceof VoidType){
            return null;
        }else return getValue(0);
    }
    @Override
    public void buildMipsTree(BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        Function mipsFunction = functionHashMap.get(irFunction);
        value irRetValue = getRetValue();
        if (irRetValue != null) { // 如果有返回值,就把返回值移入 v0
            Operand mipsRet = getCodeGen().buildOperand(irRetValue, true, irFunction, irBlock);
            Move mipsMove = new Move(V0, mipsRet);
            mipsBlock.addInstrTail(mipsMove);
        }
        Ret ret = new Ret(mipsFunction); // 然后进行弹栈和返回操作
        ret.addUseReg(null, V0); // 这里是为了窥孔优化的正确性,或许放到 readReg 里判断也行
        mipsBlock.addInstrTail(ret);
    }
    @Override
    public String toString(){
        if (getValueType() instanceof VoidType){
            return "ret void";
        }else{
            return "ret " + getValue(0).getValueType() + " " + getValue(0).getName();
        }
    }
}
