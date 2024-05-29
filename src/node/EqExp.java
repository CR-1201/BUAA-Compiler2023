package node;

import ir.instructions.Binary_Instructions.ICMP;
import ir.value;
import token.Token;

import java.io.PrintStream;
import java.util.List;

public class EqExp extends FatherNode{
    // RelExp | EqExp ('==' | '!=') RelExp

    private final List<RelExp> relExps;
    private final List<Token> eqExpTKs;

    public EqExp(List<RelExp> relExps, List<Token> eqExpTKs) {
        this.relExps = relExps;
        this.eqExpTKs = eqExpTKs;
        childrenNode.addAll(relExps);
    }

    public void output(PrintStream ps){
        relExps.get(0).output(ps);
        ps.println("<EqExp>");
        for( int i = 0 ; i < eqExpTKs.size() ; i++ ){
            ps.print(eqExpTKs.get(i).toString());
            relExps.get(i+1).output(ps);
            ps.println("<EqExp>");
        }
    }

    public List<RelExp> getRelExps() {
        return relExps;
    }

    public List<Token> getEqExpTKs() {
        return eqExpTKs;
    }

    @Override
    public void buildIrTree() {
        /*
         * 用 Icmp 将指令连缀起来
         * EqExp 是最高级的表达式,因为 LOr 和 LAnd 被翻译成了短路求值,也就是利用了 Br
         * 而 Br 要求输入是 i1,所以这就要求 EqExp 的输出是 i1
         * 与此同时,EqExp 的输入可能是 i32(AddExp 及以下) 或者是 i1 (RelExp)
         * 而翻译 EqExp 需要利用 icmp i32,所以这里才是真正需要 Zext 的地方,感觉 AddExp 反而不需要
         * RelExp 也不需要,因为它的输入一定是来自 AddExp i32
         */
        getRelExps().get(0).buildIrTree();
        value result =  FatherNode.valueUp;
        for (int i = 1; i < getRelExps().size(); i++){
            FatherNode.i32InRelUp = false;
            getRelExps().get(i).buildIrTree();
            value adder = FatherNode.valueUp;
            if (result.getValueType().isI1()) { // 如果类型不对，需要先换类型
                result = builder.buildZEXT( FatherNode.curBlock, result);
            }
            if (adder.getValueType().isI1()) {
                adder = builder.buildZEXT( FatherNode.curBlock, adder);
            }
            if (getEqExpTKs().get(i-1).getType()==Token.tokenType.EQL) {
                result = builder.buildICMP( FatherNode.curBlock, ICMP.Condition.EQ, result, adder);
            } else if (getEqExpTKs().get(i-1).getType()==Token.tokenType.NEQ) {
                result = builder.buildICMP( FatherNode.curBlock, ICMP.Condition.NE, result, adder);
            }
        }
        FatherNode.valueUp = result;
    }
}
