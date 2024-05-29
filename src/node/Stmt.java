package node;

import ir.BasicBlock;
import ir.Function;
import ir.GlobalVariable;
import ir.constants.ConstInt;
import ir.constants.ConstStr;
import ir.instructions.Memory_Instrutions.GEP;
import ir.instructions.Other_Instructions.CALL;
import ir.value;
import token.Token;
import tools.HandlePrintf;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Stmt extends FatherNode{
//         Stmt → LVal '=' Exp ';' // 每种类型的语句都要覆盖
//         | [Exp] ';' //有无Exp两种情况
//         | Block
//         | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
//         | 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
//         | 'break' ';' | 'continue' ';'
//         | 'return' [Exp] ';' // 1.有Exp 2.无Exp
//         | LVal '=' 'getint''('')'';'
//         | 'printf''('FormatString{','Exp}')'';' // 1.有Exp 2.无Exp
    private int type = 0;
    private Token ifTK;
    private Token lparentTK;
    private Cond cond = null;
    private Token rparentTK;
    private Stmt stmt_1;
    private Token elseTK;
    private Stmt stmt_2;
    private Exp exp = null;
    private Token semicnTK;
    private LVal lVal;
    private Token assignTK;
    private Block block;

    private Token forTk;
    ForStmt forStmt_1;
    ForStmt forStmt_2;
    private List<Token> semicnTKs;
    private Token TK;// break or continue
    private Token returnTK;
    private Token getintTK;
    private Token printfTK;
    private Token formatString;
    private List<Token> commas;
    private List<Exp> exps = null;

    public int getType(){
        return type;
    }
    public Stmt(int type, Token ifTK, Token lparentTK, Cond cond, Token rparentTK, Stmt stmt_1, Token elseTK, Stmt stmt_2) {
        this.type = type;
        this.ifTK = ifTK;
        this.lparentTK = lparentTK;
        this.cond = cond;
        this.rparentTK = rparentTK;
        this.stmt_1 = stmt_1;
        this.elseTK = elseTK;
        this.stmt_2 = stmt_2;
        childrenNode.add(cond);
        childrenNode.add(stmt_1);
        childrenNode.add(stmt_2);
    }

    public Stmt(int type, Exp exp, Token semicnTK) {
        this.type = type;
        this.exp = exp;
        this.semicnTK = semicnTK;
        childrenNode.add(exp);
    }

    public Stmt(int type, LVal lVal, Token assignTK, Exp exp, Token semicnTK) {
        this.type = type;
        this.exp = exp;
        this.semicnTK = semicnTK;
        this.lVal = lVal;
        this.assignTK = assignTK;
        childrenNode.add(lVal);
        childrenNode.add(exp);
    }

    public Stmt(int type, Block block) {
        this.type = type;
        this.block = block;
        childrenNode.add(block);
    }

    public Stmt(int type, Token TK, Token semicnTK) {
        this.type = type;
        this.TK = TK;
        this.semicnTK = semicnTK;
    }

    public Stmt(int type, Token returnTK, Exp exp, Token semicnTK) {
        this.type = type;
        this.returnTK = returnTK;
        this.exp = exp;
        this.semicnTK = semicnTK;
        childrenNode.add(exp);
    }

    public Stmt(int type, LVal lVal, Token assignTK, Token getintTK, Token lparentTK, Token rparentTK, Token semicnTK) {
        this.type = type;
        this.lVal = lVal;
        this.assignTK = assignTK;
        this.getintTK = getintTK;
        this.lparentTK = lparentTK;
        this.rparentTK = rparentTK;
        this.semicnTK = semicnTK;
        childrenNode.add(lVal);
    }

    public Stmt(int type, Token printfTK, Token lparentTK, Token formatString, List<Token> commas, List<Exp> exps, Token rparentTK, Token semicnTK) {
        this.type = type;
        this.printfTK = printfTK;
        this.lparentTK = lparentTK;
        this.formatString = formatString;
        this.commas = commas;
        this.exps = exps;
        this.rparentTK = rparentTK;
        this.semicnTK = semicnTK;
        childrenNode.addAll(exps);
    }

    public Stmt(int type, Token forTk, Token lparentTK, ForStmt forStmt_1, ForStmt forStmt_2, List<Token> semicnTKs, Cond cond, Token rparentTK, Stmt stmt_1) {
        this.type = type;
        this.forTk = forTk;
        this.lparentTK = lparentTK;
        this.forStmt_1 = forStmt_1;
        this.forStmt_2 = forStmt_2;
        this.semicnTKs = semicnTKs;
        this.cond = cond;
        this.rparentTK = rparentTK;
        this.stmt_1 = stmt_1;
        childrenNode.add(forStmt_1);
        childrenNode.add(cond);
        childrenNode.add(forStmt_2);
        childrenNode.add(stmt_1);
    }


    public void output(PrintStream ps) {
        switch (type) {
            case 1 -> {
                // LVal '=' Exp ';'
                lVal.output(ps);
                ps.print(assignTK.toString());
                exp.output(ps);
                ps.print(semicnTK.toString());
            }
            case 2 -> {
                // [Exp] ';'
                if (exp != null) {
                    exp.output(ps);
                }
                ps.print(semicnTK.toString());
            }
            case 3 ->
                // Block
                    block.output(ps);
            case 4 -> {
                // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                ps.print(ifTK.toString());
                ps.print(lparentTK.toString());
                cond.output(ps);
                ps.print(rparentTK.toString());
                stmt_1.output(ps);
                if (stmt_2 != null) {
                    ps.print(elseTK.toString());
                    stmt_2.output(ps);
                }
            }
            case 5 -> {
                // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
                ps.print(forTk.toString());
                ps.print(lparentTK.toString());
                if (forStmt_1 != null) {
                    forStmt_1.output(ps);
                }
                ps.print(semicnTKs.get(0).toString());
                if (cond != null) {
                    cond.output(ps);
                }
                ps.print(semicnTKs.get(1).toString());
                if (forStmt_2 != null) {
                    forStmt_2.output(ps);
                }
                ps.print(rparentTK.toString());
                stmt_1.output(ps);
            }
            case 6 -> {
                // 'break' ';' | 'continue' ';'
                ps.print(TK.toString());
                ps.print(semicnTK.toString());
            }
            case 7 -> {
                // 'return' [Exp] ';'
                ps.print(returnTK.toString());
                if (exp != null) {
                    exp.output(ps);
                }
                ps.print(semicnTK.toString());
            }
            case 8 -> {
                // LVal '=' 'getint''('')'';'
                lVal.output(ps);
                ps.print(assignTK.toString());
                ps.print(getintTK.toString());
                ps.print(lparentTK.toString());
                ps.print(rparentTK.toString());
                ps.print(semicnTK.toString());
            }
            case 9 -> {
                // 'printf''('FormatString{','Exp}')'';'
                ps.print(printfTK.toString());
                ps.print(lparentTK.toString());
                ps.print(formatString.toString());
                for (int i = 0; i < exps.size(); i++) {
                    ps.print(commas.get(i).toString());
                    exps.get(i).output(ps);
                }
                ps.print(rparentTK.toString());
                ps.print(semicnTK.toString());
            }
        }
        ps.println("<Stmt>");
    }

    public Token getReturnTK() {
        return returnTK;
    }

    public Exp getExp() {
        return exp;
    }

    public Block getBlock() {
        return block;
    }

    public Cond getCond() {
        return cond;
    }

    public Stmt getStmt_1() {
        return stmt_1;
    }
    public Stmt getStmt_2() {
        return stmt_2;
    }

    public Token getElseToken() {
        return elseTK;
    }

    public ForStmt getForStmt_1(){
        return forStmt_1;
    }

    public ForStmt getForStmt_2(){
        return forStmt_2;
    }

    public Token getToken() {
        return TK;
    }

    public LVal getLVal() {
        return lVal;
    }

    public List<Exp> getExps() {
        return exps;
    }

    public Token getFormatString() {
        return formatString;
    }
    public boolean formatStringCheck() {
        if(formatString == null )return false;
        for (int i = 1; i < formatString.getContent().length() - 1; i++) {
            char elem = formatString.getContent().charAt(i);
            if (elem == '%') {
                return formatString.getContent().charAt(i + 1) == 'd';
            } else if (elem == '\\') {
                return formatString.getContent().charAt(i + 1) == 'n';
            } else if (!(elem == 32 || elem == 33 || (elem >= 40 && elem <= 126))) {
                return false;
            }
        }
        return true;
    }
    public int formatStringParamNum(){
        if(formatString == null )return -1;
        int res = 0;
        for (int i = 1; i < formatString.getContent().length() - 1; i++) {
            if(formatString.getContent().charAt(i) == '%' && formatString.getContent().charAt(i+1) == 'd'){
                res ++;
            }
        }
        return res;
    }
    public Token getPrintfToken() {
        return printfTK;
    }

    @Override
    public void buildIrTree() {
        switch (type) {
            case 1 -> {
                // LVal '=' Exp ';'
                lVal.buildIrTree();
                value target = valueUp;
                exp.buildIrTree();
                value source = valueUp;
                // 最后是以一个 store 结尾的,说明将其存入内存,就算完成了赋值
                builder.buildSTORE(FatherNode.curBlock, source, target);
            }
            case 2 -> {
                // [Exp] ';'
                if (exp != null) {
                    exp.buildIrTree();
                }
            }
            case 3 ->
                // Block
                    block.buildIrTree();
            case 4 -> {
                // 'if' '(' Cond ')' Stmt [ 'else' Stmt ]
                /*
                 * 从这里开始的一系列东西,会出现 setBlock 的操作,只是因为为了满足短路求值
                 * 所以在条件表达式中,会被拆成多个 BasicBlock,设置他们的目的是为了保证一开始和最后的块的正确性
                 */
                BasicBlock trueBlock = builder.buildBasicBlock(FatherNode.curFunc);
                BasicBlock nextBlock = builder.buildBasicBlock(FatherNode.curFunc);
                BasicBlock falseBlock = (stmt_2 == null) ? nextBlock : builder.buildBasicBlock(FatherNode.curFunc);

                cond.setFalseBlock(falseBlock);
                cond.setTrueBlock(trueBlock);

                cond.buildIrTree();
                FatherNode.curBlock = trueBlock;
                stmt_1.buildIrTree(); // 遍历 if 块
                builder.buildBR(FatherNode.curBlock, nextBlock); // 直接跳转到 nextBlock,这是不言而喻的,因为 trueBlock 执行完就是 nextBlock
                if (stmt_2 != null) { // 对应有 else 的情况
                    FatherNode.curBlock = falseBlock;
                    stmt_2.buildIrTree();
                    builder.buildBR(FatherNode.curBlock, nextBlock);
                }
                FatherNode.curBlock = nextBlock; // 最终到了 nextBlock
            }
            case 5 -> {
                // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
                // for 涉及 4 个块
                // cond 块负责条件判断和跳转,如果是 true 则进入 bodyBlock,如果是 false 就进入 nextBlock,结束 for 语句
                // body 块是循环的主体
                BasicBlock condBlock = null;
                BasicBlock bodyBlock = builder.buildBasicBlock(FatherNode.curFunc);
                // selfBlock 自增块
                BasicBlock selfBlock = builder.buildBasicBlock(FatherNode.curFunc);
                // nextBlock 意味着循环的结束
                BasicBlock nextBlock = builder.buildBasicBlock(FatherNode.curFunc);

                loopNextBlockDown.push(nextBlock);
                loopSelfBlockDown.push(selfBlock);

                // 初始化
                if( forStmt_1 != null )forStmt_1.buildIrTree();

                if( cond != null ){
                    condBlock = builder.buildBasicBlock(FatherNode.curFunc);
                    // 先由 curBlock 进入 condBlock
                    builder.buildBR(FatherNode.curBlock, condBlock);
                    // build condBlock,有趣的是不需要再加入条件 Br,这是因为这个 Br 在 LAndExp 短路求值的时候加了
                    cond.setTrueBlock(bodyBlock);
                    cond.setFalseBlock(nextBlock);
                    FatherNode.curBlock = condBlock;
                    cond.buildIrTree();
                } else builder.buildBR(FatherNode.curBlock, bodyBlock);

                FatherNode.curBlock = bodyBlock;
                stmt_1.buildIrTree();
                builder.buildBR(FatherNode.curBlock, selfBlock);

                FatherNode.curBlock = selfBlock;
                if( forStmt_2 != null )forStmt_2.buildIrTree();
                if( cond != null ){
                    builder.buildBR(FatherNode.curBlock, condBlock);
                }else builder.buildBR(FatherNode.curBlock, bodyBlock);

                loopNextBlockDown.pop();
                loopSelfBlockDown.pop();

                FatherNode.curBlock = nextBlock;
            }
            case 6 -> {
                // 'break' ';' | 'continue' ';'
                /*
                 * 首先先做一个跳转,来跳到 loop 的下一块
                 * 然后,新作了一个块,用于使 break 后面的指令依附其上,然后失效
                 */
                if( this.TK.getType() == Token.tokenType.BREAKTK ){
                    builder.buildBR(FatherNode.curBlock, loopNextBlockDown.peek());
                }else {
                    builder.buildBR(FatherNode.curBlock, loopSelfBlockDown.peek());
                }
                FatherNode.curBlock = new BasicBlock();
            }
            case 7 -> {
                // 'return' [Exp] ';'
                if (exp != null) { // 这里也有一个和 Break 类似的操作,不知道合不合理
                    exp.buildIrTree();
                    builder.buildRET(FatherNode.curBlock, FatherNode.valueUp);
                } else {
                    builder.buildRET(FatherNode.curBlock);
                }
                FatherNode.curBlock = new BasicBlock();
            }
            case 8 -> {
                // LVal '=' 'getint''('')'';'
                lVal.buildIrTree();
                value target = FatherNode.valueUp;
                CALL source = builder.buildCALL(FatherNode.curBlock, Function.getint, new ArrayList<>());
                builder.buildSTORE(FatherNode.curBlock, source, target); // 完成赋值
            }
            case 9 -> {
                // 'printf''('FormatString{','Exp}')'';'
                ArrayList<String> strings = HandlePrintf.handleString(formatString.getContent());
                ArrayList<value> putintArgs = new ArrayList<>();// 先对参数 buildIr
                for (Exp exp: exps) {
                    exp.buildIrTree();
                    putintArgs.add(FatherNode.valueUp);
                }
                int argCur = 0;
                for (String string : strings) {
                    if (string.equals("%d")) {
                        ArrayList<value> params = new ArrayList<>();
                        params.add(putintArgs.get(argCur++));
                        builder.buildCALL(FatherNode.curBlock, Function.putint, params);
                    } else {
                        GlobalVariable globalStr = builder.buildGlobalStr(new ConstStr(string)); // 全局变量本质是 [len x i8]* 类型
                        GEP elementPtr = builder.buildGEP(curBlock, globalStr, ConstInt.ZERO, ConstInt.ZERO); // putstr 的参数是一个 i8*,所以用 GEP 降维指针
                        ArrayList<value> params = new ArrayList<>();
                        params.add(elementPtr);
                        builder.buildCALL(FatherNode.curBlock, Function.putstr, params);
                    }
                }
            }
        }
    }
}
