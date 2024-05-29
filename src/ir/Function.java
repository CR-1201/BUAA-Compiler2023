package ir;

import Pass.LoopInfo;
import backend.mipsComponent.Block;
import ir.instructions.Other_Instructions.PHI;
import ir.instructions.instruction;
import ir.types.DataType;
import ir.types.FunctionType;
import node.VarType;
import tools.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import static backend.CodeGen.*;

/**
 @author Conroy
 函数 包括 argument, basic block,有返回值的 instruction
 */
public class Function extends value{
    public static Function putstr = null;
    public static Function putint = null;
    public static Function getint = null;
    public static Function LOOP_TRASH = new Function();
    private final boolean isBuiltIn; //是否是内建函数
    private final ArrayList<Argument> arguments = new ArrayList<>(); // 函数的形参列表
    private final DataType returnType;
    private final LinkedList<value> blocks = new LinkedList<>(); // 函数使用基本块
    private LoopInfo loopInfo;
    /**
     * 存放 name-value 键值对
     * value 包括 argument, basic block,有返回值的 instruction
     */
    public final HashMap<String, value> valueSymbolTable = new HashMap<>();
    private boolean sideEffect = false; // 当一个函数向内存中写值的时候,就是有副作用的
    private final HashSet<Function> callers = new HashSet<>(); //调用图相关

    public Function(String name, FunctionType functionType, boolean isBuiltIn){
        super("@" + name, functionType, module.getModule());
        this.isBuiltIn = isBuiltIn;
        this.returnType = getValueType().getReturnType();
        for (int i = 0; i < getNumArgs(); i++){
            Argument argument = new Argument(i, functionType.getFormalArgs().get(i), this);
            arguments.add(argument);
            addFunctionSymbol(argument);
        }
    }

    /**
     * 为了解决 loop 的问题
     */
    private Function(){
        super("@LOOP_TMP", null, null);
        isBuiltIn = true;
        returnType = null;
    }
    public void setSideEffect(boolean sideEffect){
        this.sideEffect = sideEffect;
    }
    public boolean getSideEffect(){
        return sideEffect;
    }
    public HashSet<Function> getCallers(){
        return callers;
    }
    public boolean getIsBuiltIn(){
        return isBuiltIn;
    }
    public void clearCallers(){
        callers.clear();
    }
    public void addCaller(Function caller){
        callers.add(caller);
    }
    public DataType getReturnType(){
        return returnType;
    }
    public ArrayList<Argument> getArguments(){
        return arguments;
    }
    /**
     * @return 用ArrayList形式返回基本块
     */
    public ArrayList<BasicBlock> getBasicBlocksArray(){
        ArrayList<BasicBlock> result = new ArrayList<>();
        for (value blockNode : blocks){
            BasicBlock block = (BasicBlock)blockNode;
            result.add(block);
        }
        return result;
    }
    public LinkedList<value> getBasicBlocks(){
        return blocks;
    }

    @Override
    public FunctionType getValueType(){
        return (FunctionType) super.getValueType();
    }

    public void analyzeLoop() {
        if (isBuiltIn) {
            return;
        }
        for (value block : blocks) {
            ((BasicBlock)block).setParentLoop(null);
        }
        loopInfo = new LoopInfo(this); // 进行一遍图分析
    }


    /**
     * 部分信息被转移到了 type 中, value 实体只保留一小部分信息
     * @return 参数个数
     */
    public int getNumArgs(){
        return getValueType().getFormalArgs().size();
    }
    /**
     * @param value Argument | BasicBlock | Instruction(带返回值)
     */
    public void addFunctionSymbol(value value){
        valueSymbolTable.put(value.getName(), value);
    }
    /**
     * @return 函数开头基本块
     */
    public BasicBlock getFirstBlock(){
        return (BasicBlock) blocks.get(0);
    }

    public void insertTail(BasicBlock block){
        blocks.add(block);
    }
    public void insertAfter(BasicBlock block, BasicBlock after){
        for( value BasicBlockNode : blocks ){
            BasicBlock _block = (BasicBlock) BasicBlockNode;
            if( _block.equals(after) ){
                int index = blocks.indexOf(BasicBlockNode);
                blocks.add(index+1,block);
                return;
            }
        }
    }

    public void insertBefore(BasicBlock block, BasicBlock before){
        for( value BasicBlockNode : blocks ){
            BasicBlock _block = (BasicBlock) BasicBlockNode;
            if( _block.equals(before) ){
                int index = blocks.indexOf(BasicBlockNode);
                blocks.add(index,block);
                return;
            }
        }
    }

    /**
     * 在 module 的符号表中删除自己
     */
    public void eraseFromParent(){
        module.getModule().getFunctions().remove(this);
    }
    /**
     * 删除特定的 basic block
     */
    public void removeBlock(BasicBlock block){
        blocks.remove(block);
    }

    /**
     * 解析函数中的每一个块
     */
    public void buildMipsTree(){
        LinkedList<value> irBlocks = getBasicBlocks();
        for (value bNode : irBlocks) {
            BasicBlock irBlock = (BasicBlock)bNode;
            irBlock.buildMipsTree(this);
        }
    }

    public void handlePHIs() { // 因为对于 phi 的处理要涉及多个块,所以没有办法在一个块内处理
        for (value blockNode : getBasicBlocks()) { // 遍历函数中的每个块
            BasicBlock irBlock = (BasicBlock)blockNode;
            Block mipsBlock = blockBlockHashMap.get(irBlock);
            HashSet<BasicBlock> predBlocks = irBlock.getPrecursors();
            int predNum = predBlocks.size();
            if (predNum > 1) {
                ArrayList<PHI> phis = new ArrayList<>();
                for (instruction instr : irBlock.getInstructions()) {
                    if (instr instanceof PHI phi) {
                        phis.add(phi);
                    } else {
                        break;
                    }
                }
                for (BasicBlock irPreBlock : predBlocks) { // 建立基本的前驱后继查找关系
                    Pair<Block, Block> pair = new Pair<>(blockBlockHashMap.get(irPreBlock), mipsBlock);
                    phiLists.put(pair, getCodeGen().buildPHI(phis, irPreBlock, this, irBlock));
                }
            }
        }
    }
    /**
     * 编译器可以假设标记为 dso_local 的函数或变量将解析为同一链接单元中的符号
     * 即使定义不在这个编译单元内,也会生成直接访问
     * @return 一个函数的 ir
     */
    @Override
    public String toString(){
        StringBuilder s = new StringBuilder(isBuiltIn ? "declare dso_local " : "define dso_local ");
        s.append(returnType).append(" ").append(getName()).append('(');
        for (Argument argument : arguments) {
            s.append(argument.getValueType()).append(" ").append(argument.getName()).append(", ");
        }
        if (!arguments.isEmpty()){
            s.delete(s.length() - 2, s.length()); // 删除多余 comma
        }
        s.append(")");
        if (!isBuiltIn) {
            s.append(" {\n");
            for (value blockNode : blocks) {
                BasicBlock block = (BasicBlock) blockNode;
                s.append(block).append('\n');
            }
            s.append("}");
        }
        return s.toString();
    }

    public LoopInfo getLoopInfo() {
        return loopInfo;
    }
}
