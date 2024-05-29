package node;

import ir.instructions.Binary_Instructions.ICMP;
import ir.value;
import token.Token;

import java.io.PrintStream;
import java.util.List;

public class RelExp extends FatherNode{
    // RelExp -> AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
    private final List<AddExp> addExps;
    private final List<Token> relExpTKs;
    public RelExp(List<AddExp> addExps, List<Token> relExpTKs) {
        this.addExps = addExps;
        this.relExpTKs = relExpTKs;
        childrenNode.addAll(addExps);
    }

    public void output(PrintStream ps) {
        addExps.get(0).output(ps);
        ps.println("<RelExp>");
        for( int i = 0 ; i < relExpTKs.size() ; i++ ){
            ps.print(relExpTKs.get(i).toString());
            addExps.get(i+1).output(ps);
            ps.println("<RelExp>");
        }
    }

    public List<AddExp> getAddExps() {
        return addExps;
    }

    @Override
    public void buildIrTree() {
        addExps.get(0).buildIrTree();
        value result = FatherNode.valueUp;
        for (int i = 1; i < addExps.size(); i++) {
            FatherNode.i32InRelUp = false;
            addExps.get(i).buildIrTree();
            value adder = FatherNode.valueUp;
            if (result.getValueType().isI1()) { // 如果类型不对，需要先换类型
                result = builder.buildZEXT(FatherNode.curBlock, result);
            }
            if (adder.getValueType().isI1()) {
                adder =  builder.buildZEXT(FatherNode.curBlock, adder);
            }
            if (relExpTKs.get(i - 1).getType() == Token.tokenType.LEQ) {
                result = builder.buildICMP(FatherNode.curBlock, ICMP.Condition.LE, result, adder);
            } else if (relExpTKs.get(i - 1).getType() == Token.tokenType.GEQ) {
                result = builder.buildICMP(FatherNode.curBlock, ICMP.Condition.GE, result, adder);
            } else if (relExpTKs.get(i - 1).getType() == Token.tokenType.GRE) {
                result = builder.buildICMP(FatherNode.curBlock, ICMP.Condition.GT, result, adder);
            } else if (relExpTKs.get(i - 1).getType() == Token.tokenType.LSS) {
                result = builder.buildICMP(FatherNode.curBlock, ICMP.Condition.LT, result, adder);
            }
        }
        FatherNode.valueUp = result;
    }
}
