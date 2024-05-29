package Pass;

import ir.BasicBlock;

import java.util.ArrayList;

public class Loop {
    /**
     * 循环的编号计数器
     */
    private static int idCounter = 0;
    /**
     * 循环的编号
     */
    private final int id;
    /**
     * 循环头块,位于循环内部,一个循环有且仅有一个头块,一个头块可以被多个循环拥有
     * 每次循环首先执行头块,一定会执行
     * 头块支配循环中所有基本块,但是被头块支配的基本块不一定在循环中
     */
    private final BasicBlock entryBlock;
    /**
     * 循环包含的所有基本块,包括子循环的基本块
     */
    private final ArrayList<BasicBlock> allBlocks = new ArrayList<>();
    /**
     * 直接子循环,不包括子循环的子循环
     */
    private final ArrayList<Loop> subLoops = new ArrayList<>();
    /**
     * 栓块,位于循环中,其中有一个后继是头块
     */
    private final ArrayList<BasicBlock> latchBlocks = new ArrayList<>();
    /**
     * 最外层循环 parentLoop 为 null
     */
    private Loop parentLoop;
    /**
     * 最外层循环深度为 1
     */
    private int loopDepth;

    /**
     * 新建一个循环
     * @param entryBlock 入口块
     * @param latchBlocks 栓块
     */
    public Loop(BasicBlock entryBlock, ArrayList<BasicBlock> latchBlocks) {
        id = idCounter++;
        this.entryBlock = entryBlock;
        entryBlock.setParentLoop(this);
        this.latchBlocks.addAll(latchBlocks);
    }

    /**
     * 每当重新构建循环信息的时候,重置循环计数器的名字
     */
    public static void resetIdCounter() {
        idCounter = 0;
    }

    public Loop getParentLoop() {
        return parentLoop;
    }

    public BasicBlock getEntryBlock() {
        return entryBlock;
    }

    public void setParentLoop(Loop parentLoop) {
        this.parentLoop = parentLoop;
    }

    public ArrayList<Loop> getSubLoops() {
        return subLoops;
    }

    public ArrayList<BasicBlock> getAllBlocks() {
        return allBlocks;
    }

    public void addSubLoop(Loop loop) {
        if (subLoops.contains(loop)) {
            return;
        }
        subLoops.add(loop);
    }
    public void addBlock(BasicBlock block) {
        if (allBlocks.contains(block)) {
            return;
        }
        allBlocks.add(block);
    }
    public int getId() {
        return id;
    }
    public void setLoopDepth(int loopDepth) {
        this.loopDepth = loopDepth;
    }
    public int getLoopDepth() {
        return loopDepth;
    }
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(id).append("\n");
        str.append("entry:").append(entryBlock.getName()).append("\n");
        for (BasicBlock allBlock : allBlocks) {
            str.append("\t").append(allBlock.getName()).append("\n");
        }
        return str.toString();
    }
}
