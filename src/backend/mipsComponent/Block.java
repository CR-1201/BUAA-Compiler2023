package backend.mipsComponent;

import backend.mipsInstruction.Instruction;
import ir.BasicBlock;
import ir.value;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

public class Block {
    private static int index = 0; // 用来给 ObjBlock 一个独有的名字
    private final String name;

    private final LinkedList<Instruction> instructions = new LinkedList<>(); // 组成 block 的 instructions
    private Block falseSuccessor = null; // 如果最后一条指令是有条件跳转指令,那falseSuccessor就是直接后继块. false指条件跳转中不满足条件下继续执行的基本块
    private Block trueSuccessor = null; // 一个基本块最多两个后继块,如果基本块只有一个后继,那么falseSuccessor是null,trueSuccessor不是null
    private final ArrayList<Block> precursors = new ArrayList<>(); // 前驱块
    private final int loopDepth; // 记录 ir 传来的循环深度

    public void setFalseSuccessor(Block falseSuccessor) {
        this.falseSuccessor = falseSuccessor;
    }

    public void setTrueSuccessor(Block trueSuccessor) {
        this.trueSuccessor = trueSuccessor;
    }

    public Block getFalseSuccessor() {
        return falseSuccessor;
    }

    public Block getTrueSuccessor() {
        return trueSuccessor;
    }

    public String getName() {
        return name;
    }

    public int getLoopDepth() {
        return loopDepth;
    }

    public LinkedList<Instruction> getInstructions() {
        return instructions;
    }
    public Block(String name, int loopDepth) {
        this.name = name.substring(1) + "_" + index;
        index++;
        this.loopDepth = loopDepth;
    }
    /**
     * 有的 Block 可能没有对应的 irBlock,而是由 phi 构造出来的,所以没有名字
     */
    public Block(int loopDepth) {
        this.name = "transfer_" + index;
        index++;
        this.loopDepth = loopDepth;
    }

    public void addPrecursor(Block precursor) {
        this.precursors.add(precursor);
    }

    public void removePrecursor(Block precursor) {
        this.precursors.remove(precursor);
    }

    public ArrayList<Block> getPrecursor() {
        return precursors;
    }

    public void addInstrTail(Instruction armInstr) {
        instructions.add(armInstr);
    }

    public void addInstrHead(Instruction armInstr) {
        instructions.addFirst(armInstr);
    }
    /**
     *  phi 指令解析的时候会产生一大堆没有归属的 mov 指令
     *  如果这个块只有一个后继块,那么我们需要把这些 mov 指令插入到最后一条跳转指令之前,这样就可以完成 phi 的更新
     * @param phiMoves 一大堆没有归属的 move 指令
     */
    public void insertPhiMovesTail(ArrayList<Instruction> phiMoves) {
        for (Instruction phiMove : phiMoves) {
            instructions.add(instructions.size()-1,phiMove); // 插入到最后一条跳转指令之前
        }
    }
    /**
     * phiMoves 的顺序已经是正确的了,所以这个方法会确保 phiMoves 按照其原来的顺序插入到 block 的头部
     * @param phiMoves 待插入的 copy 序列
     */
    public void insertPhiCopyHead(ArrayList<Instruction> phiMoves) {
        for (int i = phiMoves.size() - 1; i >= 0; i--) {
            instructions.addFirst(phiMoves.get(i));
        }
    }
    /**
     * 这个指令一般用于去掉末尾的跳转指令,然后可以与下一个块合并
     */
    public void removeTailInstr() {
        instructions.removeLast();
    }

    public void addBefore(Instruction before,Instruction instr) {
        for( Instruction t : instructions ){
            if( t.equals(before) ){
                int index = instructions.indexOf(t);
                instructions.add(index,instr);
                return;
            }
        }
    }

    public void addAfter(Instruction after,Instruction instr) {
        for( Instruction t : instructions ){
            if( t.equals(after) ){
                int index = instructions.indexOf(t);
                instructions.add(index+1,instr);
                return;
            }
        }
    }

    public Instruction getTailInstr() {
        return instructions.getLast();
    }

    @Override
    public String toString() {
        StringBuilder blockString = new StringBuilder();
        blockString.append(name).append(":\n"); // 块标签
        for (Instruction instruction : instructions) {
            if(!instruction.toString().equals("")){
                blockString.append("\t").append(instruction);
            }
        }
        return blockString.toString();
    }


}
