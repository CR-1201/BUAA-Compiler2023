package ir.instructions.Terminator_Instructions;

import backend.mipsComponent.Block;
import backend.mipsInstruction.Branch;
import backend.mipsInstruction.CondType;
import backend.reg.Operand;
import ir.constants.ConstInt;
import ir.instructions.Binary_Instructions.ICMP;
import ir.types.VoidType;
import ir.BasicBlock;
import ir.value;

import java.util.ArrayList;
import java.util.Arrays;

import static backend.CodeGen.blockBlockHashMap;
import static backend.CodeGen.getCodeGen;
import static backend.mipsInstruction.CondType.genCond;
import static backend.mipsInstruction.CondType.getEqualOppCond;

/**
 @author Conroy
 br i1 <cond>, label <iftrue>, label <iffalse>
 br label <dest>
 FIXME
 如果让 Br 在新建的时候就维护 CFG 图,就会出现过于耦联的结果
 比如说 break 的时候会导致一个正常的块有一个 TMP_BLOCK 的前驱
 */
public class BR extends TerInstruction{
    private final boolean hasCondition;
    /**
     * 无条件跳转
     * @param target 唯一的操作数,目标基本块
     */
    public BR(BasicBlock parent, BasicBlock target){
        super(new VoidType(), parent, new ArrayList<>(){{
            add(target);
        }});
        hasCondition = false;
        if (target != null){
            parent.addSuccessor(target);
            target.addPrecursor(parent);
        }
    }
    /**
     * 有条件跳转
     * @param condition  第一个操作数,条件
     * @param trueBlock  第二个操作数,条件成立目标
     * @param falseBlock 第三个操作数,条件不成立目标
     */
    public BR(BasicBlock parent, value condition, BasicBlock trueBlock, BasicBlock falseBlock){
        super(new VoidType(), parent, new ArrayList<>(){{
            add(condition);add(trueBlock);add(falseBlock);
        }});
        hasCondition = true;
        if (trueBlock != null){
            parent.addSuccessor(trueBlock);
            trueBlock.addPrecursor(parent);
        }
        if (falseBlock != null) {
            parent.addSuccessor(falseBlock);
            falseBlock.addPrecursor(parent);
        }
    }

    public boolean getHasCondition(){
        return hasCondition;
    }

    /**
     * @return 指令的全部操作数,使用前需要用hasCondition()判定是否为有条件跳转
     */
    public ArrayList<value> getOps(){
        ArrayList<value> result = new ArrayList<>();
        int n = hasCondition ? 2 : 0;
        for( int i = 0 ; i <= n ; i++ ){
            result.add(getValue(i));
        }
        return result;
    }
    /**
     * 重载 updateValue 方法,如果设置的是跳转基本块,会自动更新所属 Block 的 successor
     * @param index 索引
     * @param newValue 新 Value
     */
    @Override
    public void updateValue(int index, value newValue){
        if (!hasCondition) {
            BasicBlock oldBlock = (BasicBlock) getValue(index);
            getParent().replaceSuccessor(oldBlock, (BasicBlock) newValue);
        }else {
            if (index > 0){
                BasicBlock oldBlock = (BasicBlock) getValue(index);
                getParent().replaceSuccessor(oldBlock, (BasicBlock) newValue);
            }
        }
        super.updateValue(index, newValue);
    }
    /**
     * 不同于 buildIcmp,没有大量的模板,而是应用了伪指令,因为 MARS 在这个部分做得很好
     */
    @Override
    public void buildMipsTree( BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        if (getHasCondition()) { // 对应有条件跳转
            value irCondition = getOps().get(0);
            BasicBlock irTrueBlock = (BasicBlock) getOps().get(1);
            BasicBlock irFalseBlock = (BasicBlock) getOps().get(2);
            if (irCondition instanceof ConstInt constInt) { // 如果条件是一个字面值,说明可以无条件跳转了
                int condNum = constInt.getValue();
                if (condNum > 0) {
                    Branch branch = new Branch(blockBlockHashMap.get(irTrueBlock));
                    mipsBlock.addInstrTail(branch);
                    mipsBlock.setTrueSuccessor(blockBlockHashMap.get(irTrueBlock));
                } else {
                    Branch branch = new Branch(blockBlockHashMap.get(irFalseBlock));
                    mipsBlock.addInstrTail(branch);
                    mipsBlock.setTrueSuccessor(blockBlockHashMap.get(irFalseBlock));
                }
            } else if (irCondition instanceof ICMP condition) { // 如果条件是 icmp
                Block mipsTrueBlock = blockBlockHashMap.get(irTrueBlock);
                Block mipsFalseBlock = blockBlockHashMap.get(irFalseBlock);
                Operand src1, src2;
                CondType cond = genCond(condition.getCondition());
                if (condition.getOp1() instanceof ConstInt && !(condition.getOp2() instanceof ConstInt)) { // 需要交换一下顺序
                    cond = getEqualOppCond(cond); // 换一下比较符号
                    src1 = getCodeGen().buildOperand(condition.getOp2(), false, irFunction, irBlock);
                    src2 = getCodeGen().buildOperand(condition.getOp1(), true, irFunction, irBlock);
                } else {
                    src1 = getCodeGen().buildOperand(condition.getOp1(), false, irFunction, irBlock);
                    src2 = getCodeGen().buildOperand(condition.getOp2(), true, irFunction, irBlock);
                }
                Branch mipsBranch = new Branch(cond, src1, src2, mipsTrueBlock); // set true_block to branch target
                mipsBlock.addInstrTail(mipsBranch);
                mipsBlock.setTrueSuccessor(mipsTrueBlock); // buildBr 的一个重要功能,登记后继
                mipsBlock.setFalseSuccessor(mipsFalseBlock);
            } else {
                System.out.println("Branch Error :" + this + ", the cond : " + irCondition);
            }
        } else { // 对应无条件跳转
            Block mipsTargetBlock = blockBlockHashMap.get((BasicBlock) getOps().get(0));
            Branch jump = new Branch(mipsTargetBlock);
            mipsBlock.addInstrTail(jump);
            mipsBlock.setTrueSuccessor(mipsTargetBlock);
        }
    }
    @Override
    public String toString(){
        StringBuilder result = new StringBuilder("br " + getValue(0).getValueType() + " " + getValue(0).getName());
        int n = hasCondition ? 2 : 0;
        for( int i = 1 ; i <= n ; i++ ){
            result.append(", ").append(getValue(i).getValueType()).append(" ").append(getValue(i).getName());
        }
        return result.toString();
    }
}
