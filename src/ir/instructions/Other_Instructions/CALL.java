package ir.instructions.Other_Instructions;

import backend.mipsComponent.Block;
import backend.mipsInstruction.*;
import backend.reg.Immediate;
import backend.reg.Operand;
import backend.reg.PhysicsReg;
import ir.BasicBlock;
import ir.instructions.instruction;
import ir.Function;
import ir.types.DataType;
import ir.types.VoidType;
import ir.value;

import java.util.ArrayList;

import static backend.CodeGen.*;
import static backend.reg.PhysicsReg.*;
import static backend.reg.PhysicsReg.V0;

/**
 @author Conroy
 <result> = call [ret attrs] <ty> <fnptrval>(<function args>)
 */
public class CALL extends instruction{
    /**
     * 有返回值的call
     * @param function 第一个操作数,被调用的函数,返回值一定不是void
     * @param args     从第二个操作数开始排列,函数参数
     */
    public CALL(int nameNum, BasicBlock parent, Function function, ArrayList<value> args){
        super("%v" + nameNum, function.getReturnType(), parent, new ArrayList<>(){{
            add(function);
            addAll(args);
        }});
    }

    /**
     * 没有返回值的call
     * @param function 同上,一定是void返回值
     * @param args     同上
     */
    public CALL( BasicBlock parent, Function function, ArrayList<value> args){
        super("", function.getReturnType(), parent, new ArrayList<>(){{
            add(function);
            addAll(args);
        }});
    }
    /**
     * @return 获得call指令被调用的函数
     */
    public Function getFunction(){
        return (Function) getValue(0);
    }
    /**
     * @return 获得call指令传递给函数的参数,全部为具体的 value,不是形参
     */
    public ArrayList<value> getArgs(){
        ArrayList<value> args = new ArrayList<>();
        int n = getFunction().getNumArgs();
        for (int i = 0; i < n; i++){
            args.add(getValue(i + 1));
        }
        return args;
    }
    @Override
    public void buildMipsTree( BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        backend.mipsComponent.Function callFunction = functionHashMap.get(getFunction());
        Instruction mipsCall;
        if (callFunction.getIsBuiltIn()) {
            mipsCall = new Comment(callFunction.getName(), false);
            mipsCall.addDefReg(null, V0); // 因为系统调用必然改变 v0
        } else {
            mipsCall = new Call(callFunction);
        }
        int argc = this.getArgs().size(); // 获取调用函数的参数数量,这里进行的是传参操作
        for (int i = 0; i < argc; i++) {
            value irArg = getArgs().get(i);
            Operand mipsSrc;
            if (i < 4) {
                mipsSrc = getCodeGen().buildOperand(irArg, true, irFunction, irBlock);
                Move mipsMove = new Move(new PhysicsReg("$a" + i), mipsSrc);
                mipsBlock.addInstrTail(mipsMove);
                mipsCall.addUseReg(null, mipsMove.getDst()); // 防止寄存器分配消除掉这些move
            } else {
                mipsSrc = getCodeGen().buildOperand(irArg, false, irFunction, irBlock);// 和上面的区别在于,这里不允许立即数的出现,必须是寄存器
                int offset = -(argc - i) * 4;
                Store store = new Store(mipsSrc, SP, new Immediate(offset));
                mipsBlock.addInstrTail(store);
            }
        }
        if (argc > 4) { // 这里进行的是栈的生长操作
            Operand mipsOffset = getCodeGen().buildConstIntOperand(4 * (argc - 4), true, irFunction, irBlock);
            Binary sub = Binary.Subu(SP, SP, mipsOffset);
            mipsBlock.addInstrTail(sub);
        }
        mipsBlock.addInstrTail(mipsCall); // 到这里才正式把 jal 指令加入
        if (argc > 4) { // 这里紧接着就是栈的恢复操作
            Operand mipsOffset = getCodeGen().buildConstIntOperand(4 * (argc - 4), true, irFunction, irBlock);
            Binary addu = Binary.Addu(SP, SP, mipsOffset);
            mipsBlock.addInstrTail(addu);
        }
        // 因为寄存器分配是以函数为单位的,所以相当于 call 指令只需要考虑在调用者函数中的影响
        // 那么 call 对应的 bl 指令会修改 lr 和 r0 (如果有返回值的话)
        // 此外，r0 - r3 是调用者保存的寄存器,这会导致可能需要额外的操作 mov ,所以这边考虑全部弄成被调用者保存
        for (int i = 0; i < 4; i++) {
            mipsCall.addDefReg(null, new PhysicsReg("$a" + i));
        }
        if (!callFunction.getIsBuiltIn()) { // 只有非内建函数需要保存 ra
            mipsCall.addDefReg(null, RA);
        }
        DataType returnType = ((getFunction())).getReturnType();// 这里是处理返回值
        mipsCall.addDefReg(null, V0); // 无论有没有返回值,都需要调用者保存 v0
        if (!(returnType instanceof VoidType)) {
            Move mipsMove = new Move(getCodeGen().buildOperand(this, false, irFunction, irBlock), V0);
            mipsBlock.addInstrTail(mipsMove);
        }
    }
    @Override
    public String toString(){
        Function function = (Function) getValue(0);
        boolean noReturn = getName().isEmpty();
        StringBuilder s = new StringBuilder(getName()).append(!noReturn ? " = call " : "call ").append(function.getReturnType()).append(' ').append(function.getName()).append('(');
        int num = function.getNumArgs();
        for (int i = 1; i <= num; i++) {
            s.append(getValue(i).getValueType()).append(' ').append(getValue(i).getName()).append(", ");
        }
        if (num > 0) {
            s.delete(s.length() - 2, s.length());
        }
        s.append(')');
        return s.toString();
    }

}
