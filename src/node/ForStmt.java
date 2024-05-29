package node;

import ir.value;
import token.Token;

import java.io.PrintStream;

public class ForStmt extends FatherNode{
    // ForStmt → LVal '=' Exp
    private final LVal lVal;
    private final Token assignTK;
    private final Exp exp;
    public ForStmt(LVal lVal, Token assignTK, Exp exp) {
        this.lVal = lVal;
        this.assignTK = assignTK;
        this.exp = exp;
        childrenNode.add(lVal);
        childrenNode.add(exp);
    }

    public void output(PrintStream ps){
        lVal.output(ps);
        ps.print(assignTK.toString());
        exp.output(ps);
        ps.println("<ForStmt>");
    }

    public Exp getExp() {
        return exp;
    }

    public LVal getLVal() {
        return lVal;
    }

    @Override
    public void buildIrTree() {
        // LVal '=' Exp ';'
        lVal.buildIrTree();
        value target = valueUp;
        exp.buildIrTree();
        value source = valueUp;
        // 最后是以一个 store 结尾的,说明将其存入内存,就算完成了赋值
        builder.buildSTORE(FatherNode.curBlock, source, target);
    }
}
