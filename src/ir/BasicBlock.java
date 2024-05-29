package ir;

import Pass.Loop;
import ir.instructions.instruction;
import ir.types.LabelType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

/**
 @author Conroy
 一个基本块由若干 instruction 组成,且最后一条指令一定为终结指令 ( ret / br )
 支持 3 种插入指令的方式(插在末尾,插在某个指令之前,插在头部)
 Attention : BasicBlock 不是 user,它并不使用 Instruction
 */
public class BasicBlock extends value{
    private final LinkedList<instruction> instructions = new LinkedList<>();
    /**
     * 前驱与后继基本块,不讲求顺序,因此不用链表
     */
    private final HashSet<BasicBlock> precursors = new HashSet<>();
    private final HashSet<BasicBlock> successors = new HashSet<>();
    private final ArrayList<BasicBlock> dominators = new ArrayList<>(); // 当前基本块的必经基本块
    private final ArrayList<BasicBlock> i_dominators = new ArrayList<>(); // 当前基本块直接支配的基本块
    private BasicBlock directDom;// 直接支配当前基本块的基本块
    private int domLevel; // 在支配树中的深度
    private final HashSet<BasicBlock> dominanceFrontier = new HashSet<>(); // 支配边际,即刚好不被当前基本块支配的基本块
    public LinkedList<instruction> getInstructions(){
        return instructions;
    }
    /**
     * 当前块所在的循环
     * 如果为 null,那么说明当前块不在循环中
     */
    private Loop parentLoop;
    /**
     * @param nameNum 基本块的名字,一定为数字编号
     * @param parent  基本块所在函数
     */
    public BasicBlock(int nameNum, Function parent){
        super("%b" + nameNum, new LabelType(), parent);
        parent.addFunctionSymbol(this);
    }
    /**
     * 这个 block 主要用于控制 break 和 continue 后的语句
     */
    public BasicBlock(){
        super("%LOOP_TMP", new LabelType(), Function.LOOP_TRASH);
    }
    /**
     * BasicBlock 的 parent 一定是 Function
     * @return Function parent
     */
    @Override
    public Function getParent(){
        return (Function) super.getParent();
    }
    public HashSet<BasicBlock> getPrecursors(){
        return precursors;
    }
    public void insertTail(instruction instruction){
        instructions.add(instruction);
    }
    public void insertBefore(instruction instruction, instruction before){
        for( instruction instructionNode : instructions ){
            if( instructionNode.equals(before) ){
                int index = instructions.indexOf(instructionNode);
                instructions.add(index-1,before);
                return;
            }
        }
    }
    public void insertHead(instruction instruction){
        instructions.add(0,instruction);
    }
    public void eraseInstruction(instruction instruction) {
        instructions.remove(instruction);
    }

    public void removeInstruction(instruction instruction) {
        instructions.remove(instruction);
    }
    public void addPrecursor(BasicBlock precursor){
        precursors.add(precursor);
    }
    public void addSuccessor(BasicBlock successor){
        successors.add(successor);
    }
    /**
     * precursor - successor 是一对双向关系
     * @param oldBlock 原有 block
     * @param newBlock 现有 block
     */
    public void replaceSuccessor(BasicBlock oldBlock, BasicBlock newBlock){
        successors.remove(oldBlock);
        oldBlock.precursors.remove(this);
        successors.add(newBlock);
//        newBlock.precursors.add(this);
    }
    /**
     * 获得结尾的指令,如果结尾没有指令,那么返回 null
     * @return 结尾指令
     */
    public instruction getTailInstruction(){
        if (instructions.isEmpty()){
            return null;
        }else{
            return instructions.getLast();
        }
    }
    /**
     * 返回当前块是否是 other 的支配者
     * @param other 另一个块
     * @return 是则为 true
     */
    public boolean isDominator(BasicBlock other){
        return other.dominators.contains(this);
    }
    public void setIDirectDom(BasicBlock directDom){
        this.directDom = directDom;
    }
    public void setDomLevel(int domLevel) {
        this.domLevel = domLevel;
    }
    public ArrayList<BasicBlock> getI_Dominators(){
        return i_dominators;
    }
    public ArrayList<BasicBlock> getDominators(){
        return dominators;
    }
    public int getDomLevel() {
        return domLevel;
    }
    public HashSet<BasicBlock> getDominanceFrontier(){
        return dominanceFrontier;
    }
    public HashSet<BasicBlock> getSuccessors(){
        return successors;
    }
    public BasicBlock getDirectDom(){
        return directDom;
    }
    public ArrayList<instruction> getInstructionsArray(){
        return new ArrayList<>(instructions);
    }
    /**
     * 获得循环深度
     * 如果不在循环中,则深度为 1
     * @return 循环深度
     */
    public int getLoopDepth() {
        if (parentLoop == null) {
            return 0;
        }
        return parentLoop.getLoopDepth();
    }

    public void setParentLoop(Loop parentLoop) {
        this.parentLoop = parentLoop;
    }

    public Loop getParentLoop() {
        return parentLoop;
    }
    /**
     * 移除基本块
     */
    public void eraseFromParent(){
        getParent().removeBlock(this);
    }
    /**
     * 按顺序解析当前基本块内的每个指令
     * @param function 当前函数
     */
    public void buildMipsTree(Function function){
        for (instruction instr : getInstructions()) {
            instr.buildMipsTree(this, function);
        }
    }
    @Override
    public String toString(){
        // 将 % 去掉,因为只有在跳转指令里需要加上这个
        StringBuilder s = new StringBuilder(getName().substring(1)).append(":\n");
        for (instruction instructionNode : instructions){
            s.append('\t').append(instructionNode).append('\n');
        }
        if (!instructions.isEmpty()) {
            s.deleteCharAt(s.length() - 1);
        }
        return s.toString();
    }
}
