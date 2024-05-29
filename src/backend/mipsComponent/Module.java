package backend.mipsComponent;

import java.util.ArrayList;

public class Module {
    private final ArrayList<Function> functions = new ArrayList<>(); // 管理所有的函数
    private final ArrayList<GlobalVariable> globalVariables = new ArrayList<>(); // 管理所有的全局变量
    private Function mainFunction; // 主函数

    public void addGlobalVariable(GlobalVariable objGlobalVariable) {
        globalVariables.add(objGlobalVariable);
    }

    public void addFunction(Function function) {
        if (function.getName().equals("main")) {
            this.mainFunction = function;
        }
        functions.add(function);
    }

    public ArrayList<Function> getFunctions() {
        return functions;
    }

    public ArrayList<Function> getNoBuiltInFunctions() {
        ArrayList<Function> noBuiltIns = new ArrayList<>();
        for (Function function : functions) {
            if (!function.getIsBuiltIn()) {
                noBuiltIns.add(function);
            }
        }
        return noBuiltIns;
    }

    public Function getMainFunction() {
        return mainFunction;
    }
    /**
     * 打印：
     * 数据段各个全局变量
     * 跳转到 main 的语句和结束（_start）
     * 两个内建函数的打印
     * 非内建函数打印
     * @return 模块汇编
     */
    @Override
    public String toString() {
        StringBuilder module = new StringBuilder("# Conroy 20375337\n");
        //  <---- macro ---->
        // putint
        module.append(".macro putint\n");
        module.append("\tli $v0, 1\n");
        module.append("\tsyscall\n");
        module.append(".end_macro\n\n");
        // getint
        module.append(".macro getint\n");
        module.append("\tli $v0, 5\n");
        module.append("\tsyscall\n");
        module.append(".end_macro\n\n");
        // putstr
        module.append(".macro putstr\n");
        module.append("\tli $v0, 4\n");
        module.append("\tsyscall\n");
        module.append(".end_macro\n\n");
        //  <---- data segment ---->
        module.append(".data\n");

        for (GlobalVariable globalVariable : globalVariables) { // .word
            if (globalVariable.isAlign()) {
                module.append(globalVariable).append("\n");
            }
        }
        for (GlobalVariable globalVariable : globalVariables) {  // .space
            if (!globalVariable.isAlign() && !globalVariable.hasInit()) {
                module.append(globalVariable).append("\n");
            }
        }
        for (GlobalVariable globalVariable : globalVariables) { // .ascizz
            if (!globalVariable.isAlign() && globalVariable.hasInit()) {
                module.append(globalVariable).append("\n");
            }
        }
        //  <---- text segment ---->
        module.append(".text\n"); // 首先先跳到 main 函数执行
        // 打印函数
        module.append(mainFunction); // 首先打印主函数
        for (Function function : functions) { // 打印其他函数
            if (!function.getIsBuiltIn() && function != mainFunction) {
                module.append(function).append("\n");
            }
        }
        return module.toString();
    }
}
