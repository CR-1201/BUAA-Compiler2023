package Pass;

import ir.*;
import ir.instructions.Memory_Instrutions.ALLOCA;
import ir.instructions.Other_Instructions.CALL;
import ir.instructions.instruction;
import ir.types.IntType;
import ir.types.PointerType;
import ir.types.valueType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class GlobalVariableLocalize implements Pass{
    private final HashMap<GlobalVariable, HashSet<Function>> functionUsers = new HashMap<>(); //记录的是全局变量和调用它的函数列表
    private final module irModule = module.getModule();
    private final HashMap<Function, ArrayList<Function>> callers = new HashMap<>(); // 函数和它调用的函数
    private final HashMap<Function, ArrayList<Function>> callees = new HashMap<>(); // 函数和调用它的函数
    @Override
    public void pass() {
        analyzeGlobalUse();
        buildCallGraph();
        localize();
    }
    private void analyzeGlobalUse() {
        for (value gv : irModule.getGlobalVariables()) { // 遍历所有的全局向量
            GlobalVariable globalVariable = (GlobalVariable) gv;
            for (user user : globalVariable.getUsers()) { // 遍历所有的使用者
                if (user instanceof instruction userInst) { // 如果使用者是一条指令（应该是显然的）
                    Function userFunc = userInst.getParent().getParent();
                    if (!functionUsers.containsKey(globalVariable)) {
                        functionUsers.put(globalVariable, new HashSet<>());
                    }
                    if (!userFunc.equals(Function.LOOP_TRASH)) {
                        functionUsers.get(globalVariable).add(userFunc);
                    }
                }
            }
        }
    }
    /**
     * 通过遍历 irModule,获得所有的调用和被调用关系
     */
    private void buildCallGraph() {
        callers.clear();
        callees.clear();
        for (Function curFunc : irModule.getFunctionsArray()) {
            if (!curFunc.getIsBuiltIn()) {
                if (!callers.containsKey(curFunc)) {
                    ArrayList<Function> arrayList = new ArrayList<>();
                    callers.put(curFunc, arrayList);
                }
                for (BasicBlock curBB : curFunc.getBasicBlocksArray()) { // Calculate all the call instruction in this function
                    for (instruction curInst : curBB.getInstructionsArray()) {
                        if (curInst instanceof CALL curCall) {
                            Function callee = curCall.getFunction(); // 被调用的函数
                            if (!callee.getIsBuiltIn()) { // 如果被调用的函数不是内建函数
                                if (!callees.containsKey(callee)) {  // 新建数组
                                    ArrayList<Function> arrayList = new ArrayList<>();
                                    callees.put(callee, arrayList);
                                }
                                if (!callers.get(curFunc).contains(callee)) {
                                    callers.get(curFunc).add(callee);
                                }
                                if (!callees.get(callee).contains(curFunc)) {
                                    callees.get(callee).add(curFunc);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private void localize() {
        LinkedList<value> globalVariables = new LinkedList<>(irModule.getGlobalVariables());
        for (value gv : globalVariables) {
            GlobalVariable curGlobal = (GlobalVariable) gv;
            if (!functionUsers.containsKey(curGlobal) || functionUsers.get(curGlobal).isEmpty()) { // 如果没有任何一个函数使用这个全局变量
                irModule.getGlobalVariables().remove(curGlobal);
                continue;
            }
            if (functionUsers.containsKey(curGlobal) && functionUsers.get(curGlobal).size() == 1) { // Only used by one function
                Function targetFunc = functionUsers.get(curGlobal).iterator().next(); // 获得这个函数
                BasicBlock entryBlock = targetFunc.getFirstBlock(); // 插入头块
                if (callees.containsKey(targetFunc) || !callees.getOrDefault(targetFunc, new ArrayList<>()).isEmpty()) {
                    continue;
                }
                valueType globalType = ((PointerType) curGlobal.getValueType()).getPointeeType();
                if (globalType instanceof IntType) {
                    ALLOCA alloca = irBuilder.getIrBuilder().buildALLOCA(globalType, entryBlock); // 建立一个 alloca
                    for (instruction beforeInst : entryBlock.getInstructionsArray()) {
                        if (!(beforeInst instanceof ALLOCA)) { // 建立一个 store
                            irBuilder.getIrBuilder().buildSTOREBeforeInstr(entryBlock, curGlobal.getInitVal(), alloca, beforeInst);
                            break;
                        }
                    }
                    curGlobal.selfReplace(alloca);  // 用 alloca 代替
                    irModule.getGlobalVariables().remove(curGlobal);
                }
            }
        }
    }
}
