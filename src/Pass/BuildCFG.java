package Pass;

import ir.BasicBlock;
import ir.Function;
import ir.instructions.Terminator_Instructions.BR;
import ir.instructions.instruction;
import ir.module;
import ir.value;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Vector;

public class BuildCFG implements Pass {
    @Override
    public void pass() {
        for (value node : module.getModule().getFunctions()) {
            Function func = (Function) node;
            if (!func.getIsBuiltIn()) {
                runBasicBlockPredSucc(func);
            }
        }
    }
    private void addEdge(BasicBlock pred, BasicBlock succ) {
        pred.addSuccessor(succ);
        succ.addPrecursor(pred);
    }
    private void clear(Function func) {
        for (value node : func.getBasicBlocks()) {
            BasicBlock block = (BasicBlock) node;
            block.getSuccessors().clear();
            block.getPrecursors().clear();
        }
    }
    private void runBasicBlockPredSucc(Function func){
        clear(func);
        BasicBlock entry = (BasicBlock) func.getBasicBlocks().getFirst();
        dfsBlock(entry);
        clearUselessBlock(func);
    }

    private final HashSet<BasicBlock> visited = new HashSet<>();

    private void dfsBlock(BasicBlock curBlock) {
        visited.add(curBlock);
        instruction instr = curBlock.getInstructions().getLast();
        if (instr instanceof BR br) {
            if (br.getHasCondition()) {
                BasicBlock trueBlock = (BasicBlock) br.getValue(1);
                addEdge(curBlock, trueBlock);
                if (!visited.contains(trueBlock)) {
                    dfsBlock(trueBlock);
                }
                BasicBlock falseBlock = (BasicBlock) br.getValue(2);
                addEdge(curBlock, falseBlock);
                if (!visited.contains(falseBlock)) {
                    dfsBlock(falseBlock);
                }
            } else {
                BasicBlock nextBlock = (BasicBlock) br.getValue(0);
                addEdge(curBlock, nextBlock);
                if (!visited.contains(nextBlock)) {
                    dfsBlock(nextBlock);
                }
            }
        }
    }
    /**
     * 注意这里的是并不严谨的,只是删除了前驱为 0 的节点,并且更新了其后继的前驱节点
     * 但是如果更新后的后继也成了前驱为 0 的节点,那么就无能为力了,可以考虑用一个不动点去优化
     * @param func 当前函数
     */
    private void clearUselessBlock(Function func) {
        BasicBlock entry = (BasicBlock) func.getBasicBlocks().getFirst();
        LinkedList<value> basicBlocks = new LinkedList<>(func.getBasicBlocks());
        for (value node : basicBlocks) {
            BasicBlock block = (BasicBlock) node;
            if (block.getPrecursors().isEmpty() && block != entry) {
                HashSet<BasicBlock> successors = new HashSet<>(block.getSuccessors());
                for (BasicBlock successor : successors) {
                    successor.getPrecursors().remove(block);
                }
                LinkedList<instruction> instructions = new LinkedList<>(block.getInstructions());
                for (instruction instr : instructions) {
                    instr.removeAllOperators();
                    instr.eraseFromParent();
                }
                func.removeBlock(block);
            }
        }
    }
}
