package ir;

import backend.mipsComponent.Block;

import java.util.ArrayList;
import java.util.LinkedList;

import static backend.CodeGen.*;
import static config.Config.irError;
/**
 @author Conroy
   编译单元 单例模式
   一个编译单元由若干个函数与全局变量组成
   parent->null,name->"Module",valueType->null
 */
public class module extends value{
    private static final module module = new module();
    private module() {
        super("Module", null, null);
    }
    public static module getModule(){
        return module;
    }
    /**
     * 这两个list是双向链表,用于存储函数与全局变量,是 module 的符号表
     */
    private final LinkedList<value> functions = new LinkedList<>();
    private final LinkedList<value> globalVariables = new LinkedList<>();
    /**
     * 向 module 中加入函数
     */
    public void addFunction(Function function){
        for (value functionNode : functions){
            if (functionNode.equals(function) && irError){
                throw new AssertionError("function is already in!");
            }
        }
        functions.add(function);
    }
    /**
     * 向 module 中加入全局变量
     */
    public void addGlobalVariable(GlobalVariable globalVariable){
        for (value globalVariableNode : globalVariables){
            if (globalVariableNode.equals(globalVariable)){
                throw new AssertionError("global variable is already in!");
            }
        }
        globalVariables.add(globalVariable);
    }
    /**
     * 这两个get...s()方法只能由后端调用
     */
    public LinkedList<value> getFunctions(){
        return functions;
    }

    public LinkedList<value> getGlobalVariables(){
        return globalVariables;
    }
    /**
     * 这两个get()方法前后端都可以调用
     */
    public value getGlobalVariable(String name){
        for (value globalVariableNode : globalVariables){
            if (globalVariableNode.getName().equals(name)){
                return globalVariableNode ;
            }
        }
        throw new AssertionError("global variable " + name + " not found!");
    }
    public value getFunction(String name){
        for (value functionNode : functions){
            if (functionNode.getName().equals(name)){
                return functionNode;
            }
        }
        throw new AssertionError("function " + name + " not found!");
    }

    public ArrayList<Function> getFunctionsArray(){
        ArrayList<Function> result = new ArrayList<>();
        for (value functionNode : functions){
            result.add((Function) functionNode); // 强制转化不会出问题
        }
        return result;
    }
    /**
     * 分析全局变量们
     */
    public void buildGlobalVariables() {
        LinkedList<value> irGlobalVariables = getGlobalVariables();
        for (value node : irGlobalVariables) {
            ir.GlobalVariable irGlobalVariable = (ir.GlobalVariable)node;
            backend.mipsComponent.GlobalVariable mipsGlobalVariable = irGlobalVariable.buildMipsTree();
            getCodeGen().getMipsModule().addGlobalVariable(mipsGlobalVariable);
        }
    }
    /**
     * 这个函数会初步完成对于所有 block 的构造,并且登记到 blockBlockHashMap
     * 之所以要在开始解析之前完成这个步骤,是因为对于 block 来说,需要知悉前驱块,后继块
     * 但是这些块有可能还没有被解析,所以可能无法登记,所以先将所有的 block 构造完成,方便之后细致解析
     * 除此之外,还需要进行前驱块的登记,这是因为 mips 前驱块的编号必须与 ir 前驱块编号保持一致
     * 在 mips 中,如果想要化到 ir 中,那么 functionHashMap 在最外层, block 在 function 那一层即可
     */
    private void irMap() {
        LinkedList<value> irFunctions = getFunctions();
        for (value fNode : irFunctions) {
            ir.Function irFunction = (ir.Function)fNode;
            backend.mipsComponent.Function mipsFunction = new backend.mipsComponent.Function(irFunction.getName(), irFunction.getIsBuiltIn());

            functionHashMap.put(irFunction, mipsFunction); // 构建映射
            getCodeGen().getMipsModule().addFunction(mipsFunction); // 加入函数

            LinkedList<value> irBlocks = irFunction.getBasicBlocks();
            for (value bNode : irBlocks) { // 建立新块,最后才进行块的序列化
                ir.BasicBlock irBlock = (ir.BasicBlock)bNode;
                Block mipsBlock = new Block(irBlock.getName(), irBlock.getLoopDepth());
                blockBlockHashMap.put(irBlock, mipsBlock);
            }
            for (value bNode : irBlocks) { // 完成前驱映射
                ir.BasicBlock irBlock = (ir.BasicBlock)bNode;
                Block mipsBlock = blockBlockHashMap.get(irBlock);
                for (ir.BasicBlock precursor : irBlock.getPrecursors()) {
                    mipsBlock.addPrecursor(blockBlockHashMap.get(precursor));
                }
            }
        }
    }

    /**
     * 首先应该进行 irMap 操作,即进行首次遍历
     * 每个非内建函数都需要先被解析, 然后处理 Phi（插入 Phi 指令）
     * 最后进行块的序列化（即将 block 加入）
     */
    public void buildFunctions() {
        irMap();
        LinkedList<value> irFunctions = getFunctions();
        for (value node : irFunctions) {
            ir.Function irFunction = (ir.Function)node;
            if (!irFunction.getIsBuiltIn()) { // 只有非内建函数才需要解析
                irFunction.buildMipsTree();
                irFunction.handlePHIs();
                functionHashMap.get(irFunction).blockSerial(blockBlockHashMap.get((BasicBlock)irFunction.getBasicBlocks().getFirst()), phiLists); // 进行块的序列化
            }
        }
    }

    @Override
    public String toString(){
        StringBuilder s = new StringBuilder();
        for (value globalVariableNode : globalVariables){
            GlobalVariable globalVariable = (GlobalVariable) globalVariableNode;
            s.append(globalVariable).append('\n');
        }
        s.append("\n");
        for (value functionNode : functions){
            Function function = (Function)functionNode;
            s.append(function).append('\n');
        }
        return s.toString();
    }
}
