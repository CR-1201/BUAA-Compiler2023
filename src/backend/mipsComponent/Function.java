package backend.mipsComponent;

import backend.mipsInstruction.Branch;
import backend.mipsInstruction.CondType;
import backend.mipsInstruction.Instruction;
import backend.reg.Immediate;
import backend.reg.PhysicsReg;
import backend.reg.Reg;
import backend.reg.VirtualReg;
import tools.Pair;

import java.util.*;

public class Function {
    private final String name; // 函数名
    private final LinkedList<Block> Blocks = new LinkedList<>();
    private final HashSet<VirtualReg> usedVirtualRegs = new HashSet<>(); // 这里记录着这个函数用到的所有虚拟整型寄存器,最终目的是进行寄存器分配
    private int allocaSize; // 包括 2 个部分, alloca 和 spill
    private int totalStackSize;
    private final TreeSet<Integer> calleeSavedRegIndexes = new TreeSet<>(); // 这是函数需要在调用前保存的寄存器, 只要函数用到了这个寄存器,而且不是 a0, a1 这样的传参寄存器,那么就都是需要保存的
    private final HashSet<Block> hasSerial = new HashSet<>(); // 是一个辅助量,用于在 DFS 序列化的时候作为 visit
    private final HashSet<Immediate> argOffsets = new HashSet<>(); // 这是该函数需要使用栈上的参数的时候使用到的 mov 指令,来控制 offset
    private final boolean isBuiltIn;

    public Function(String name, Boolean isBuiltIn) {
        this.name = name.substring(1); // 去除 @
        this.isBuiltIn = isBuiltIn;
        this.allocaSize = 0;
    }
    public String getName() {
        return name;
    }

    public boolean getIsBuiltIn() {
        return isBuiltIn;
    }

    public void addUsedVirReg(VirtualReg virtualReg) {
        usedVirtualRegs.add(virtualReg);
    }

    public void addArgOffset(Immediate offset) {
        argOffsets.add(offset);
    }

    public int getTotalStackSize() {
        return totalStackSize;
    }

    public HashSet<VirtualReg> getUsedVirtualRegs() {
        return usedVirtualRegs;
    }

    public void addAllocaSize(int size) {
        allocaSize += size;
    }

    public int getAllocaSize() {
        return allocaSize;
    }

    public TreeSet<Integer> getCalleeSavedRegIndexes() {
        return calleeSavedRegIndexes;
    }

    public LinkedList<Block> getBlocks() {
        return Blocks;
    }
    /**
     * 栈上的空间,从上到下:
     *  调用者保存的寄存器空间
     *  <----------------->
     *  局部变量
     *  <----------------->
     *  未能存入寄存器的参数 (alloca 空间)
     *  <----------------->
     *  参数空间
     *  <----------------->
     */
    public void fixStack() {
        for (Block block : Blocks) {
            for (Instruction instr : block.getInstructions()) {
                for (Reg defReg : instr.getRegDef()) { // 只有写了寄存器才需要保存
                    int num = ((PhysicsReg) defReg).getNum();
                    if (PhysicsReg.calleeSavedReg.contains(num)) {
                        calleeSavedRegIndexes.add(num);
                    }
                }
            }
        }
        int stackRegSize = 4 * calleeSavedRegIndexes.size();
        totalStackSize = stackRegSize + allocaSize;
        for (Immediate argOffset : argOffsets) {
            int newOffset = argOffset.getImmediate() + totalStackSize;
            argOffset.setImmediate(newOffset);
        }
    }
    /**
     * 这个函数用于处理非直接后继块 (这个后继块不打算放在当前块的正后面),在这里是处理 true 后继块
     * 对于这种块,不能把 copy 指令放到 cur 里, 所以要么插入 successor,要么再做一个块
     * 之所以不能的原因是不会直接放在 cur 的后面
     * @param curBlock 当前块
     * @param successorBlock 间接后继
     * @param phiCopy copy
     */
    private void handleTrueCopy(Block curBlock, Block successorBlock, ArrayList<Instruction> phiCopy) {
        if (!phiCopy.isEmpty()) { // 如果没有 copy
            if (successorBlock.getPrecursor().size() == 1) { // 如果后继块前只有一个前驱块（当前块）,那么就可以直接插入到后继块的最开始
                successorBlock.insertPhiCopyHead(phiCopy);
            } else { // 如果后继块前有多个前驱块（无法确定从哪个块来）,那么就应该新形成一个块
                Block transferBlock = new Block(curBlock.getLoopDepth()); // 新做出一个中转块
                transferBlock.insertPhiCopyHead(phiCopy); // 把 phiMov 指令放到这里
                Branch transferJump = new Branch(successorBlock); // 做出一个中转块跳转到的指令
                transferBlock.addInstrTail(transferJump);

                transferBlock.setTrueSuccessor(successorBlock); // transfer 登记前驱后继
                transferBlock.addPrecursor(curBlock);

                successorBlock.removePrecursor(curBlock); // successor 登记前驱后继
                successorBlock.addPrecursor(transferBlock);

                curBlock.setTrueSuccessor(transferBlock); // cur 登记前驱后继

                Branch tailInstr = (Branch) curBlock.getTailInstr(); // 修改 cur 的最后一条指令
                tailInstr.setTarget(transferBlock);
            }
        }
    }
    /**
     * 这里处理的是直接后继块,不会做一个新块,而是将 copy 直接插入 cur 的末尾
     * 如果后继还没有放置,那么就放置后继
     * 如果已经放置,那么就再做一个 jump
     * @param curBlock 当前块
     * @param successorBlock 后继块
     * @param phiCopy copy
     */
    private void handleFalseCopy(Block curBlock, Block successorBlock, ArrayList<Instruction> phiCopy) {
        for (Instruction phi_copy : phiCopy) {
            curBlock.addInstrTail(phi_copy);
        }
        if (hasSerial.contains(successorBlock)) { // 如果已经序列化了,那么还需要增加一条 branch 指令,跳转到已经序列化的后继块上
            Branch tailInstr = new Branch(successorBlock);
            curBlock.addInstrTail(tailInstr);
        }
    }
    /**
     * 这个函数用于交换当前块的两个后继,交换操作很简单
     * 是为了让 false 块是未序列化块的几率更大
     * 或者让 false 与 curBlock 间有 copy (在 false 和 true 均被序列化后)
     * @param curBlock 当前块
     * @param phiWaitLists phi copy
     */
    private void swapSuccessorBlock(Block curBlock, HashMap<Pair<Block, Block>, ArrayList<Instruction>> phiWaitLists) {
        Block trueSuccessor = curBlock.getTrueSuccessor();
        Block falseSuccessor = curBlock.getFalseSuccessor();
        Pair<Block, Block> falseLookUp = new Pair<>(curBlock, falseSuccessor);
        if (!hasSerial.contains(trueSuccessor) ||
                hasSerial.contains(trueSuccessor) && hasSerial.contains(falseSuccessor) && (!phiWaitLists.containsKey(falseLookUp) || phiWaitLists.get(falseLookUp).isEmpty())) {
            curBlock.setTrueSuccessor(falseSuccessor);
            curBlock.setFalseSuccessor(trueSuccessor);
            Branch tailBranch = (Branch) curBlock.getTailInstr();
            CondType cond = tailBranch.getCond();
            tailBranch.setCond(CondType.getOppCond(cond));
            tailBranch.setTarget(curBlock.getTrueSuccessor()); // 这里注意,不能直接用 trueBlock
        }
    }
    /**
     * 本质是一个 DFS
     * 当存在两个后继块的时候,优先放置 false 块
     * @param curBlock 当前块
     * @param phiWaitLists 记录 phi 的表
     */
    public void blockSerial(Block curBlock, HashMap<Pair<Block, Block>, ArrayList<Instruction>> phiWaitLists) {
        hasSerial.add(curBlock); // 登记
        Blocks.addLast(curBlock); // 插入当前块,就是序列化当前块

        if (curBlock.getTrueSuccessor() == null && curBlock.getFalseSuccessor() == null) return; // 如果没有后继了,那么就结束

        if (curBlock.getFalseSuccessor() == null) { // 如果没有错误后继块,说明只有一个后继块,那么就应该考虑与当前块合并
            Block successorBlock = curBlock.getTrueSuccessor();
            Pair<Block, Block> trueLookup = new Pair<>(curBlock, successorBlock); // 这个前驱后继关系用于查询有多少个 phiMove 要插入,一个后继块,直接将这些指令插入到跳转之前即可
            curBlock.insertPhiMovesTail(phiWaitLists.getOrDefault(trueLookup, new ArrayList<>()));
            if (!hasSerial.contains(successorBlock)) { // 合并的条件是后继块还未被序列化,此时只需要将当前块最后一条跳转指令移除掉就好了
                curBlock.removeTailInstr();
                blockSerial(successorBlock, phiWaitLists);
            } // 但是不一定能够被合并成功,因为又可以后继块已经被先序列化了,那么就啥都不需要干了
        } else { // 如果有两个后继块
            swapSuccessorBlock(curBlock, phiWaitLists); // 交换块的目的是为了让处理更加快捷
            Block trueSuccessorBlock = curBlock.getTrueSuccessor();
            Block falseSuccessorBlock = curBlock.getFalseSuccessor();
            Pair<Block, Block> trueLookup = new Pair<>(curBlock, trueSuccessorBlock);
            Pair<Block, Block> falseLookup = new Pair<>(curBlock, falseSuccessorBlock);

            handleTrueCopy(curBlock, trueSuccessorBlock, phiWaitLists.getOrDefault(trueLookup, new ArrayList<>()));

            handleFalseCopy(curBlock, falseSuccessorBlock, phiWaitLists.getOrDefault(falseLookup, new ArrayList<>()));

            if (!hasSerial.contains(curBlock.getFalseSuccessor())) {
                blockSerial(curBlock.getFalseSuccessor(), phiWaitLists);
            }
            if (!hasSerial.contains(curBlock.getTrueSuccessor())) {
                blockSerial(curBlock.getTrueSuccessor(), phiWaitLists);
            }
        }
    }

    /**
     * 需要打印：
     * 函数 label
     * 保存被调用者寄存器
     * 移动栈指针 sp
     * 所以基本块
     * @return 函数汇编
     */
    @Override
    public String toString() {
        StringBuilder funcString = new StringBuilder();
        funcString.append(name).append(":\n");
        if (!name.equals("main")) { // 只有非主函数才需要保存寄存器
            int stackOffset = -4; // 调用者保存的寄存器
            for (Integer savedRegIndex : calleeSavedRegIndexes) {
                funcString.append("\t").append("sw ").append(new PhysicsReg(savedRegIndex)).append(", ").append(stackOffset).append("($sp)\n");
                stackOffset -= 4; // 下移 4
            }
        }
        if (totalStackSize != 0) { // 移动栈指针
            funcString.append("\tadd $sp, $sp, ").append(-totalStackSize).append("\n");
        }
        for (Block block : Blocks) { // 遍历所有基本块
            funcString.append(block);
        }
        return funcString.toString();
    }

}
