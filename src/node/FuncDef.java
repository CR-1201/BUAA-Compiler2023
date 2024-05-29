package node;

import ir.Argument;
import ir.BasicBlock;
import ir.constants.ConstInt;
import ir.instructions.Memory_Instrutions.ALLOCA;
import ir.instructions.Terminator_Instructions.BR;
import ir.instructions.Terminator_Instructions.RET;
import ir.instructions.instruction;
import ir.types.DataType;
import ir.types.FunctionType;
import ir.types.IntType;
import ir.types.VoidType;
import token.Token;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class FuncDef extends FatherNode{
    // FuncDef -> FuncType Ident '(' [FuncFParams] ')' Block
    // FuncFParams → FuncFParam { ',' FuncFParam }
    private final FuncType funcType;
    private final Token ident;
    private final Token lparent;
    private final List<FuncFParam> funcFParams;
    private final List<Token> commas;
    private final Token rparent;
    private final Block block;

    public FuncDef(FuncType funcType, Token ident, Token lparent, List<FuncFParam> funcFParams, List<Token> commas, Token rparent, Block block) {
        this.funcType = funcType;
        this.ident = ident;
        this.lparent = lparent;
        this.funcFParams = funcFParams;
        this.commas = commas;
        this.rparent = rparent;
        this.block = block;
        if (funcType != null) childrenNode.add(funcType);
        if (funcFParams != null) childrenNode.addAll(funcFParams);
        if (block != null) childrenNode.add(block);
    }

    public void output(PrintStream ps) {
        funcType.output(ps);
        ps.print(ident.toString());
        ps.print(lparent.toString());
        if( funcFParams.size() > 0 ){
            funcFParams.get(0).output(ps);
        }
        for( int i = 0 ; i < commas.size() ; i++ ){
            ps.print(commas.get(i).toString());
            funcFParams.get(i+1).output(ps);
        }
        if( funcFParams.size() > 0 ){
            ps.println("<FuncFParams>");
        }
        ps.print(rparent.toString());
        block.output(ps);
        ps.println("<FuncDef>");
    }

    public Token getIdent() {
        return ident;
    }

    public List<FuncFParam> getFuncFParams() {
        return funcFParams;
    }

    public FuncType getFuncType() {
        return funcType;
    }

    public Block getBlock() {
        return block;
    }

    public boolean isReturn() {
        int size = block.getBlockItems().size();
        if(size >= 1){
            BlockItem blockItem = block.getBlockItems().get(size - 1);
            Stmt stmt = blockItem.getStmt();
            if (stmt != null)
                return stmt.getType() == 7;
        }
        return false;
    }

    public int getEndLine(){
        return block.getRbrace().getLineNum();
    }

    /**
     * 这个是为了让形参是 SSA 形式
     * 必须为形参分配空间,然后将指针指向形参
     * 不然形参可能直接被更改
     */
    public void buildFParamsSSA() {
        ArrayList<Argument> args = FatherNode.curFunc.getArguments();
        for (int i = 0; i < funcFParams.size(); i++) {
            FuncFParam funcFParam = funcFParams.get(i);
//            System.out.println("参数"+ i + ": "+funcFParam.getIdent().getContent());
            Argument argument = args.get(i);
            // 这里建立 alloca 和 store 指令,而且并不需要分类讨论,因为类型在之前已经探讨过了
            ALLOCA alloca = builder.buildALLOCA(argument.getValueType(), FatherNode.curBlock);
            builder.buildSTORE(FatherNode.curBlock, argument, alloca);
            irSymbolTable.addValue(funcFParam.getIdent().getContent(), alloca);
        }
    }

    @Override
    public void buildIrTree() {
        String funcName = getIdent().getContent(); // 函数名
        DataType returnType;
        if( getFuncType().getType().getType() == Token.tokenType.VOIDTK ){
            returnType = new VoidType();
        }else returnType = new IntType(32); // 获取函数返回类型
        ArrayList<DataType> argsType = new ArrayList<>(); // 此处这是 buildFunc,但是为了 SSA 特性,之后还需要再次遍历 funcFParams 来为形参分配空间
        if (funcFParams != null) {
            ArrayList<DataType> types = new ArrayList<>();
            for (FuncFParam funcFParam : funcFParams) {
                funcFParam.buildIrTree();
                types.add(FatherNode.argTypeUp);
            }
            FatherNode.argTypeArrayUp = types;
            argsType.addAll(types);
        }
        FatherNode.curFunc = builder.buildFunction(funcName, new FunctionType(argsType, returnType), false); // build function object
        irSymbolTable.addValue(funcName, FatherNode.curFunc); // add to symbol table
        BasicBlock entryBlock = builder.buildBasicBlock(FatherNode.curFunc); // 在 entryBlock 加入函数的形参
        irSymbolTable.pushFuncLayer(); // 进入一个函数,就会加一层
        // 将函数的形参放到 block 中,将对 Function 的 arg 的初始化 delay 到 visit(ctx.block)
        FatherNode.curBlock = entryBlock;
        // 如果参数列表不为空,说明是需要参数 alloc 的
        if (funcFParams != null) {
            buildFParamsSSA();
        }
        irSymbolTable.pushBlockLayer();
        block.buildIrTree(); // 建立函数体
        irSymbolTable.popBlockLayer();

        // 在解析完了函数后,开始处理善后工作
        // 如果没有默认的 return 语句 (其实错误处理已经保证了结尾有一个 return 语句）
        instruction tailInstr = FatherNode.curBlock.getTailInstruction();
        // 结尾没有指令或者指令不是跳转指令, null 指令被包含了
        if (!(tailInstr instanceof RET || tailInstr instanceof BR)) {
            if (FatherNode.curFunc.getReturnType() instanceof VoidType) {
                builder.buildRET(FatherNode.curBlock);
            } else {
                builder.buildRET(FatherNode.curBlock, ConstInt.ZERO);
            }
        }

        irSymbolTable.popFuncLayer();
    }
}
