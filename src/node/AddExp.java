package node;

import ir.constants.ConstInt;
import ir.value;
import symbol.Symbol;
import symbol.SymbolTable;
import token.Token;

import java.io.PrintStream;
import java.util.List;

public class AddExp extends FatherNode{
    // AddExp -> MulExp | AddExp ('+' | '−') MulExp
    private final List<MulExp> mulExps;

    private final List<Token> addExpTKs;

    public AddExp(List<MulExp> mulExps, List<Token> addExpTKs) {
        this.mulExps = mulExps;
        this.addExpTKs = addExpTKs;
        childrenNode.addAll(mulExps);
    }

    public void output(PrintStream ps){
        mulExps.get(0).output(ps);
        ps.println("<AddExp>");
        for( int i = 0 ; i < addExpTKs.size() ; i++ ){
            ps.print(addExpTKs.get(i).toString());
            mulExps.get(i+1).output(ps);
            ps.println("<AddExp>");
        }
    }

    public List<Token> getAddExpTKs() {
        return addExpTKs;
    }

    public List<MulExp> getMulExps() {
        return mulExps;
    }

    public Symbol.Type getType(SymbolTable symbolTable) {
        return mulExps.get(0).getType(symbolTable);
    }
    @Override
    public void buildIrTree(){
        /*
         * 分为两种:
         * 第一种是可以直接计算类型的,那么就需要在这里进行加减法计算
         * 第二种不可以直接计算,那么就需要在这里添加 add, sub 的指令
         * 对于 1 + (1 == 0) 的式子,虽然应该不会出现,但是我依然写了,对于 (1 == 0),需要用 zext 拓展后参与运算
         */
        if ( FatherNode.canCalValueDown ){  // 如果是可计算的，那么就要算出来
            getMulExps().get(0).buildIrTree(); // 先分析一个
            int sum = FatherNode.valueIntUp;
            for (int i = 1; i < getMulExps().size(); i++){
                getMulExps().get(i).buildIrTree();
                if (getAddExpTKs().get(i - 1).getType() == Token.tokenType.PLUS ){
                    sum += FatherNode.valueIntUp;
                }else if( getAddExpTKs().get(i - 1).getType() == Token.tokenType.MINU ){
                    sum -= FatherNode.valueIntUp;
                }
            }
            FatherNode.valueIntUp = sum;
            FatherNode.valueUp = new ConstInt(FatherNode.valueIntUp);
        } else { // 是不可直接计算的,要用表达式
            getMulExps().get(0).buildIrTree(); // 先分析一个
            value sum = FatherNode.valueUp;
            if ( sum.getValueType().isI1()) { // 如果类型不对，需要先换类型
                sum = builder.buildZEXT(FatherNode.curBlock, sum);
            }
            for (int i = 1; i < getMulExps().size(); i++){
                getMulExps().get(i).buildIrTree();
                value adder = FatherNode.valueUp;
                if (adder.getValueType().isI1()){
                    adder = builder.buildZEXT(FatherNode.curBlock, adder);
                }
                if ( getAddExpTKs().get(i - 1).getType() == Token.tokenType.PLUS ){
                    sum = builder.buildADD(FatherNode.curBlock, sum, adder);
                } else if( getAddExpTKs().get(i - 1).getType() == Token.tokenType.MINU ){
                    sum = builder.buildSUB(FatherNode.curBlock, sum, adder);
                }
            }
            FatherNode.valueUp = sum;
        }
    }
}
