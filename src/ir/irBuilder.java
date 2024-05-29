package ir;

import ir.constants.ConstArray;
import ir.constants.ConstStr;
import ir.constants.Constant;
import ir.instructions.Binary_Instructions.*;
import ir.instructions.Memory_Instrutions.ALLOCA;
import ir.instructions.Memory_Instrutions.GEP;
import ir.instructions.Memory_Instrutions.LOAD;
import ir.instructions.Memory_Instrutions.STORE;
import ir.instructions.Other_Instructions.CALL;
import ir.instructions.Other_Instructions.PHI;
import ir.instructions.Other_Instructions.ZEXT;
import ir.instructions.Terminator_Instructions.BR;
import ir.instructions.Terminator_Instructions.RET;
import ir.instructions.instruction;
import ir.types.DataType;
import ir.types.FunctionType;
import ir.types.VoidType;
import ir.types.valueType;
import node.FatherNode;

import java.util.ArrayList;
import java.util.HashMap;

public class irBuilder {
    // 唯一实例
    private static final irBuilder irBuilder = new irBuilder();

    public static irBuilder getIrBuilder(){
        return irBuilder;
    }
    public void buildModule(FatherNode root) {
        root.buildIrTree();
    }
    /**
     * module 实例
     */
    public final module module = ir.module.getModule();
    /**
     * 一个起名计数器, 对于 instruction 或者 BasicBlock 没有名字
     * 需要用计数器取一个独一无二的名字
     */
    private static int nameNumCounter = 0;

    private static int strNumCounter = 0;
    /**
     * 用于给 phi 一个名字,可以从 0 开始编号,因为 phi 一定是 %p1 之类的
     */
    public static int phiNameNum = 0;
    private static final HashMap<String, GlobalVariable> globalStrings = new HashMap<>();
    /**
     * 全局变量初始化的时候,一定是用常量初始化的
     * 建造一个全局变量,并将其加入 module
     * @param ident 标识符
     * @param initValue 初始值
     * @param isConst 是否是常量
     * @return 全局变量
     */
    public GlobalVariable buildGlobalVariable(String ident, Constant initValue, boolean isConst){
        GlobalVariable globalVariable = new GlobalVariable(ident, initValue, isConst);
        module.addGlobalVariable(globalVariable);
        return globalVariable;
    }
    public GlobalVariable buildGlobalStr(ConstStr initValue){
        if (globalStrings.containsKey(initValue.getContent())){
            return globalStrings.get(initValue.getContent());
        }else{
            GlobalVariable globalVariable = new GlobalVariable("Str" + strNumCounter++, initValue, true);
            module.addGlobalVariable(globalVariable);
            globalStrings.put(initValue.getContent(), globalVariable);
            return globalVariable;
        }
    }
    public Function buildFunction(String ident, FunctionType type, boolean isBuiltIn){ // 是否是内联函数
        Function function = new Function(ident, type, isBuiltIn);
        module.addFunction(function);
        return function;
    }
    public BasicBlock buildBasicBlock(Function function){
        BasicBlock block = new BasicBlock(nameNumCounter++, function);
        function.insertTail(block);
        return block;
    }
    public BasicBlock buildBlockAfter(Function function, BasicBlock after){
        BasicBlock block = new BasicBlock(nameNumCounter++, function);
        function.insertAfter(block, after);
        return block;
    }
    public ADD buildADD(BasicBlock parent, value src1, value src2){
        ADD add = new ADD(nameNumCounter++, parent, src1, src2);
        parent.insertTail(add);
        return add;
    }
    public SUB buildSUB(BasicBlock parent, value src1, value src2){
        SUB sub = new SUB(nameNumCounter++, parent, src1, src2);
        parent.insertTail(sub);
        return sub;
    }
    public ADD buildADDBefore(BasicBlock parent, value src1, value src2, instruction nextOp){
        ADD add = new ADD(nameNumCounter++, parent, src1, src2);
        parent.insertBefore(add, nextOp);
        return add;
    }
    public SUB buildSUBBefore(BasicBlock parent, value src1, value src2, instruction nextOp){
        SUB sub = new SUB(nameNumCounter++, parent, src1, src2);
        parent.insertBefore(sub, nextOp);
        return sub;
    }
    public MUL buildMUL(BasicBlock parent, value src1, value src2) {
        MUL mul = new MUL(nameNumCounter++, parent, src1, src2);
        parent.insertTail(mul);
        return mul;
    }
    public MUL buildMULBefore(BasicBlock parent, value lhs, value rhs, instruction nextOp){
        MUL mul = new MUL(nameNumCounter++, parent, lhs, rhs);
        parent.insertBefore(mul, nextOp);
        return mul;
    }
    public SDIV buildSDIV(BasicBlock parent, value src1, value src2){
        SDIV sdiv = new SDIV(nameNumCounter++, parent, src1, src2);
        parent.insertTail(sdiv);
        return sdiv;
    }
    public SREM buildSREM(BasicBlock parent, value src1, value src2){
        SREM srem = new SREM(nameNumCounter++, parent, src1, src2);
        parent.insertTail(srem);
        return srem;
    }
    public ICMP buildICMP(BasicBlock parent, ICMP.Condition condition, value src1, value src2) {
        ICMP icmp = new ICMP(nameNumCounter++, parent, condition, src1, src2);
        parent.insertTail(icmp);
        return icmp;
    }
    public ZEXT buildZEXT(BasicBlock parent, value value){
        ZEXT zext = new ZEXT(nameNumCounter++, parent, value);
        parent.insertTail(zext);
        return zext;
    }
    /**
     * 为了方便 mem2reg 优化,约定所有的 Alloca 放到每个函数的入口块处
     * @param allocatedType alloca 空间的类型
     * @param parent 基本块
     * @return Alloca 指令
     */
    public ALLOCA buildALLOCA(valueType allocatedType, BasicBlock parent){
        BasicBlock realParent = parent.getParent().getFirstBlock();
        ALLOCA alloca = new ALLOCA(nameNumCounter++, allocatedType, realParent);
        realParent.insertHead(alloca);
        return alloca;
    }
    /**
     * 为了方便 mem2reg 优化,约定所有的 Alloca 放到每个函数的入口块处
     * ConstAlloca 对应的是局部的常量数组的 Alloca 这种 Alloca 会多存储一个常量数组 ConstArray
     * 用于 a[constA[0]] 这种阴间情况,此时是没法用常量访存,其实还有很多情况,我之前使用的是 cannotCalDown 比较不本质
     * @param allocatedType alloca 空间的类型
     * @param parent 基本块
     * @return Alloca 指令
     */
    public ALLOCA buildALLOCA(valueType allocatedType, BasicBlock parent, ConstArray initVal){
        BasicBlock realParent = parent.getParent().getFirstBlock();
        ALLOCA alloca = new ALLOCA(nameNumCounter++, allocatedType, realParent,initVal);
        realParent.insertHead(alloca);
        return alloca;
    }
    /**
     * 全新的 GEP 指令,可以允许变长的 index
     * @param parent 基本块
     * @param base 基地址（是一个指针）
     * @param index 变长索引
     * @return 一个新的指针
     */
    public GEP buildGEP(BasicBlock parent, value base, value... index) {
        int nameNum = nameNumCounter++;
        GEP gep;
        if (index.length == 1){
            gep = new GEP(nameNum, parent, base, index[0]);
        }else{
            gep = new GEP(nameNum, parent, base, index[0], index[1]);
        }
        parent.insertTail(gep);
        return gep;
    }
    /**
     * @param parent 基本块
     * @param content 存储内容
     * @param addr 地址
     */
    public void buildSTORE(BasicBlock parent, value content, value addr){
        STORE store = new STORE(parent, content, addr);
        parent.insertTail(store);
    }

    public LOAD buildLOAD(BasicBlock parent, value addr){
        LOAD load = new LOAD(nameNumCounter++, parent, addr);
        parent.insertTail(load);
        return load;
    }
    public void buildRET(BasicBlock parent, value... retValue){
        RET ret;
        // 没有返回值
        if (retValue.length == 0) {
            ret = new RET(parent);
        }else{
            ret = new RET(parent, retValue[0]);
        }
        parent.insertTail(ret);
    }
    public BR buildBR(BasicBlock parent, BasicBlock target){
        BR br = new BR(parent, target);
        parent.insertTail(br);
        return br;
    }
    public void buildBR(BasicBlock parent, value condition, BasicBlock trueBlock, BasicBlock falseBlock){
        BR br = new BR(parent, condition, trueBlock, falseBlock);
        parent.insertTail(br);
    }
    public void buildBRBeforeInstr(BasicBlock parent, BasicBlock nextBlock, instruction before){
        BR ans = new BR(parent, nextBlock);
        parent.insertBefore(ans, before);
    }
    public PHI buildPHI(DataType type, BasicBlock parent){
        PHI phi = new PHI(phiNameNum++, type, parent, parent.getPrecursors().size());
        parent.insertHead(phi);
        return phi;
    }
    public PHI buildPHI(DataType type, BasicBlock parent, int precursorNum){
        PHI phi = new PHI(phiNameNum++, type, parent, precursorNum);
        parent.insertHead(phi);
        return phi;
    }
    public void buildSTOREBeforeInstr(BasicBlock parent, value val, value location, instruction before){
        STORE ans = new STORE(parent, val, location);
        parent.insertBefore(ans, before);
    }
    public CALL buildCALL(BasicBlock parent, Function function, ArrayList<value> args){
        CALL call;
        // 没有返回值
        if (function.getReturnType() instanceof VoidType) {
            call = new CALL(parent, function, args);
            parent.insertTail(call);
        }else{
            call = new CALL(nameNumCounter++, parent, function, args);
            parent.insertTail(call);
        }
        return call;
    }
}
