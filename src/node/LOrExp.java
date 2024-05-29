package node;

import ir.BasicBlock;
import token.Token;

import java.io.PrintStream;
import java.util.List;

public class LOrExp extends FatherNode{
    // LOrExp -> LAndExp | LOrExp '||' LAndExp
    private final List<LAndExp> lAndExps;
    private final List<Token> lOrExpTKs;

    private BasicBlock trueBlock = null;
    private BasicBlock falseBlock = null;
    public void setTrueBlock(BasicBlock trueBlock) {
        this.trueBlock = trueBlock;
    }

    public void setFalseBlock(BasicBlock falseBlock) {
        this.falseBlock = falseBlock;
    }
    public LOrExp(List<LAndExp> lAndExps, List<Token> lOrExpTKs) {
        this.lAndExps = lAndExps;
        this.lOrExpTKs = lOrExpTKs;
        childrenNode.addAll(lAndExps);
    }

    public void output(PrintStream ps) {
        lAndExps.get(0).output(ps);
        ps.println("<LOrExp>");
        for( int i = 0 ; i < lOrExpTKs.size() ; i++ ){
            ps.print(lOrExpTKs.get(i).toString());
            lAndExps.get(i+1).output(ps);
            ps.println("<LOrExp>");
        }
    }

    public List<LAndExp> getLAndExps() {
        return lAndExps;
    }

    /**
     * 所谓的短路求值,描述的是这样的一个过程,对于由 && 连接的多个表达式
     * 当某个表达式被求值的时候,如果是 true,那么不会直接进入 trueBlock,
     * 而是会进入一个新的 block,在这个 block 里会判断下一个表达式
     * 但是如果是 false,那么就会直接进入 falseBlock
     * 对于 || 连接的多个表达式
     * 当某个表达式被求值的时候,如果是 true,那么会直接进入 trueBlock,
     * 如果是 false,则会进入一个新的 block 判断下一个表达式
     */
    @Override
    public void buildIrTree() {
        // TODO 突然意识到,break 后接 if 可能导致新的块又被造了出来,这些指令都是没有用的,不过似乎影响不大,起码基本块的性质保证了
        for (int i = 0; i < lAndExps.size() - 1; i++) {
            LAndExp lAndExp = lAndExps.get(i);
            lAndExp.setTrueBlock(trueBlock);

            BasicBlock nextBlock = builder.buildBasicBlock(FatherNode.curFunc);
            lAndExp.setFalseBlock(nextBlock);
            lAndExp.buildIrTree();
            FatherNode.curBlock = nextBlock;
        }

        LAndExp tailExp = lAndExps.get(lAndExps.size() - 1);
        tailExp.setTrueBlock(trueBlock);
        tailExp.setFalseBlock(falseBlock);
        tailExp.buildIrTree();
    }
}
