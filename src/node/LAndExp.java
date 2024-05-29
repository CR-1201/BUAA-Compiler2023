package node;

import ir.BasicBlock;
import ir.constants.ConstInt;
import ir.instructions.Binary_Instructions.ICMP;
import token.Token;

import java.io.PrintStream;
import java.util.List;

public class LAndExp extends FatherNode{
    // LAndExp → EqExp | LAndExp '&&' EqExp
    private final List<EqExp> eqExps;
    private final List<Token> lAndExpTKs;

    private BasicBlock trueBlock = null;
    private BasicBlock falseBlock = null;
    public LAndExp(List<EqExp> eqExps, List<Token> lAndExpTKs) {
        this.eqExps = eqExps;
        this.lAndExpTKs = lAndExpTKs;
        childrenNode.addAll(eqExps);
    }

    public void setTrueBlock(BasicBlock trueBlock)
    {
        this.trueBlock = trueBlock;
    }

    public void setFalseBlock(BasicBlock falseBlock)
    {
        this.falseBlock = falseBlock;
    }

    public void output(PrintStream ps){
        eqExps.get(0).output(ps);
        ps.println("<LAndExp>");
        for( int i = 0 ; i < lAndExpTKs.size() ; i++ ){
            ps.print(lAndExpTKs.get(i).toString());
            eqExps.get(i+1).output(ps);
            ps.println("<LAndExp>");
        }
    }

    public List<EqExp> getEqExps() {
        return eqExps;
    }

    @Override
    public void buildIrTree() {
        for (EqExp eqExp : eqExps) {
            BasicBlock nextBlock = builder.buildBasicBlock(FatherNode.curFunc);
            FatherNode.i32InRelUp = true;
            eqExp.buildIrTree();
            if (FatherNode.i32InRelUp) { // 在这里,将某个为 I32 的 eqExp 变成 I1
                FatherNode.i32InRelUp = false;
                FatherNode.valueUp = builder.buildICMP(FatherNode.curBlock, ICMP.Condition.NE, FatherNode.valueUp, ConstInt.ZERO);
            }
            builder.buildBR(FatherNode.curBlock, FatherNode.valueUp, nextBlock, falseBlock); // 错了就直接进入 falseBlock
            FatherNode.curBlock = nextBlock;
        }
        builder.buildBR(FatherNode.curBlock, trueBlock);
    }
}
