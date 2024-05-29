package backend.mipsInstruction;

import backend.mipsComponent.Function;
import backend.reg.PhysicsReg;

import java.util.TreeSet;

public class Ret extends Instruction{
    private final Function function;

    public Ret(Function function) {
        this.function = function;
    }
    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();
        int stackSize = function.getTotalStackSize();
        if (stackSize != 0) { // 处理一下 alloc 的部分
            ret.append("add $sp,  $sp, ").append(stackSize).append("\n");
        }
        if (function.getName().equals("main")) { // 如果是主函数就直接结束运行,并且没有保存寄存器的操作
            ret.append("\tli $v0, 10\n");
            ret.append("\tsyscall\n\n");
        } else {
            TreeSet<Integer> calleeSavedRegIndexes = function.getCalleeSavedRegIndexes();
            int stackOffset = -4;
            for (Integer regIndex : calleeSavedRegIndexes) {
                ret.append("\t").append("lw ").append(new PhysicsReg(regIndex)).append(", ").append(stackOffset).append("($sp)\n");
                stackOffset -= 4; // 下移 4
            }
            ret.append("\tjr $ra\n");
        }
        return ret.toString();
    }
}
