package node;

import ir.Function;
import ir.types.*;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class CompUnit extends FatherNode{
    // CompUnit -> {Decl} {FuncDef} MainFuncDef
    private final List<Decl> decls;
    private final List<FuncDef> funcDefs;
    private final MainFuncDef mainFuncDef;

    public CompUnit(List<Decl> decls, List<FuncDef> funcDefs, MainFuncDef mainFuncDef) {
        this.decls = decls;
        this.funcDefs = funcDefs;
        this.mainFuncDef = mainFuncDef;
        childrenNode.addAll(decls);
        childrenNode.addAll(funcDefs);
        childrenNode.add(mainFuncDef);
    }

    public List<Decl> getDecl() {
        return decls;
    }

    public List<FuncDef> getFuncDef() {
        return funcDefs;
    }

    public MainFuncDef getMainFuncDef() {
        return mainFuncDef;
    }

    //重定向
    public void output(PrintStream ps) {
        decls.forEach(decl -> decl.output(ps));
        funcDefs.forEach(funcDef -> funcDef.output(ps));
        mainFuncDef.output(ps);
        ps.println("<CompUnit>");
    }

    /**
     * 需要加入 3 个 IO 函数
     * 最后还是没有加入符号表，这依赖于程序是正确的
     */
    @Override
    public void buildIrTree() {
        ArrayList<DataType> printfArgs = new ArrayList<>();
        printfArgs.add(new PointerType(new IntType(8)));
        Function.putstr = builder.buildFunction("putstr", new FunctionType(printfArgs, new VoidType()), true);
        ArrayList<DataType> putintArgs = new ArrayList<>();
        putintArgs.add(new IntType(32));
        Function.putint = builder.buildFunction("putint", new FunctionType(putintArgs, new VoidType()), true);
        Function.getint = builder.buildFunction("getint", new FunctionType(new ArrayList<>(), new IntType(32)), true);

        for (FatherNode fatherNode : childrenNode) {
            fatherNode.buildIrTree();
        }
    }



}
