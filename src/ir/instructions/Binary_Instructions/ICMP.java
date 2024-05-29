package ir.instructions.Binary_Instructions;

import ir.BasicBlock;
import ir.types.IntType;
import ir.value;
/**
 @author Conroy
 <result> = icmp <cond> <ty> <op1>, <op2>
 */
public class ICMP extends BinInstruction{
    public enum Condition{
        EQ, LE, LT, GE, GT, NE;
        @Override
        public String toString(){
            return switch (this) {
                case EQ -> "eq";
                case GE -> "sge";
                case GT -> "sgt";
                case LE -> "sle";
                case LT -> "slt";
                default -> "ne";
            };
        }
    }
    private final Condition condition;

    /**
     * @param condition 判断类型
     * @param op1       第一个操作数
     * @param op2       第二个操作数
     */
    public ICMP(int nameNum, BasicBlock parent, Condition condition, value op1, value op2){
        super(nameNum, new IntType(1), parent, op1, op2);
        this.condition = condition;
    }

    public Condition getCondition(){
        return condition;
    }

    @Override
    public boolean isCommutative(){
        return condition.equals(Condition.NE) || condition.equals(Condition.EQ);
    }

    @Override
    public String toString(){
        return getName() + " = icmp " + condition.toString() + " " +
                getValue(0).getValueType() + " " + getValue(0).getName() + ", " + getValue(1).getName();
    }
}
