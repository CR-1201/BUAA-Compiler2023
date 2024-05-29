package node;

import ir.BasicBlock;

import java.io.PrintStream;

public class Cond extends FatherNode{
    // Cond -> LOrExp
    private final LOrExp lOrExp;
    private BasicBlock trueBlock = null;
    private BasicBlock falseBlock = null;

    public void setTrueBlock(BasicBlock trueBlock) {
        this.trueBlock = trueBlock;
    }

    public void setFalseBlock(BasicBlock falseBlock) {
        this.falseBlock = falseBlock;
    }

    public BasicBlock getFalseBlock() {
        return falseBlock;
    }

    public BasicBlock getTrueBlock() {
        return trueBlock;
    }

    public Cond(LOrExp lOrExp) {
        this.lOrExp = lOrExp;
        childrenNode.add(lOrExp);
    }

    public void output(PrintStream ps){
        lOrExp.output(ps);
        ps.println("<Cond>");
    }

    public LOrExp getLOrExp() {
        return lOrExp;
    }

    @Override
    public void buildIrTree() {
        lOrExp.setTrueBlock(trueBlock);
        lOrExp.setFalseBlock(falseBlock);
        lOrExp.buildIrTree();
    }
}
