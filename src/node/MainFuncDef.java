package node;

import ir.BasicBlock;
import ir.constants.ConstInt;
import ir.instructions.Terminator_Instructions.BR;
import ir.instructions.Terminator_Instructions.RET;
import ir.instructions.instruction;
import ir.types.DataType;
import ir.types.FunctionType;
import ir.types.IntType;
import token.Token;

import java.io.PrintStream;
import java.util.ArrayList;

public class MainFuncDef extends FatherNode{
    // MainFuncDef -> 'int' 'main' '(' ')' Block
    private final Token intTK;
    private final Token mainTK;
    private final Token lparent;
    private final Token rparent;
    private final Block block;
    public MainFuncDef(Token intTK, Token mainTK, Token lparent, Token rparent, Block block) {
        this.intTK = intTK;
        this.mainTK = mainTK;
        this.lparent = lparent;
        this.rparent = rparent;
        this.block = block;
        childrenNode.add(block);
    }

    public void output(PrintStream ps) {
        ps.print(intTK.toString());
        ps.print(mainTK.toString());
        ps.print(lparent.toString());
        ps.print(rparent.toString());
        block.output(ps);
        ps.println("<MainFuncDef>");
    }

    public Block getBlock() {
        return block;
    }

    public Token getIdent() {
        return mainTK;
    }
    public Token getIntTK(){
        return intTK;
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

    public Integer getEndLine() {
        return block.getRbrace().getLineNum();
    }

    @Override
    public void buildIrTree() {
        String funcName = "main";
        DataType returnType = new IntType(32);
        // 此处这是 buildFunc,但是为了 SSA 特性,之后还需要再次遍历 funcFParams 来为形参分配空间
        ArrayList<DataType> argsType = new ArrayList<>();
        FatherNode.curFunc = builder.buildFunction(funcName, new FunctionType(argsType, returnType), false);
        FatherNode.irSymbolTable.addValue(funcName, FatherNode.curFunc);
        // 在 entryBlock 加入函数的形参
        BasicBlock entryBlock = builder.buildBasicBlock(FatherNode.curFunc);
        // 进入一个函数,就会加一层
        FatherNode.irSymbolTable.pushFuncLayer();
        // 将函数的形参放到 block 中,将对 Function 的 arg 的初始化 delay 到 visit(ctx.block)
        FatherNode.curBlock = entryBlock;
        // 建立函数体
        block.buildIrTree();
        // 在解析完了函数后,开始处理善后工作
        // 如果没有默认的 return 语句
        instruction tailInstr = FatherNode.curBlock.getTailInstruction();
        // 结尾没有指令或者指令不是跳转指令,null 指令被包含了
        if (!(tailInstr instanceof RET || tailInstr instanceof BR)) {
            builder.buildRET(curBlock, ConstInt.ZERO);
        }
        FatherNode.irSymbolTable.popFuncLayer();
    }
}
