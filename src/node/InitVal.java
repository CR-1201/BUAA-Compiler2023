package node;

import ir.constants.ConstInt;
import ir.value;
import token.Token;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class InitVal extends FatherNode{
    // InitVal -> Exp | '{' [ InitVal { ',' InitVal } ] '}'
    private Token lbrace;
    private List<InitVal> initVals;
    private List<Token> commas;
    private Token rbrace;
    private Exp exp;
    private final ArrayList<Integer> dims = new ArrayList<>();
    public InitVal(Token lbrace, List<InitVal> initVals, List<Token> commas, Token rbrace) {
        this.lbrace = lbrace;
        this.initVals = initVals;
        this.commas = commas;
        this.rbrace = rbrace;
        childrenNode.addAll(initVals);
    }
    public void setDims(ArrayList<Integer> dims)
    {
        this.dims.addAll(dims);
    }

    public InitVal(Exp exp) {
        this.exp = exp;
        childrenNode.add(exp);
    }

    public void output(PrintStream ps){
        if( exp == null ){
            ps.print(lbrace.toString());
            if(initVals.size() > 0){
                initVals.get(0).output(ps);
                for( int i = 0 ; i < commas.size() ; i++ ){
                    ps.print(commas.get(i).toString());
                    initVals.get(i+1).output(ps);
                }
            }
            ps.print(rbrace.toString());
        }else{
            exp.output(ps);
        }
        ps.println("<InitVal>");
    }

    public Exp getExp() {
        return exp;
    }

    public List<InitVal> getInitVals() {
        return initVals;
    }

    @Override
    public void buildIrTree() {
        /*
         * 对于单变量初始值,通过 valueUp 返回
         * 对于数组初始值,通过 valueArrayUp 返回
         * 之所以无法像 ConstInit 都用 valueUp 返回,是因为对于变量初始值,没有一个 ConstArray 这样的结构打包
         */
        if (exp != null) { // 初始值是一个表达式（单变量）
            if (FatherNode.globalInitDown) { // 在进行全局单变量初始化
                FatherNode.canCalValueDown = true;
                exp.buildIrTree();
                FatherNode.canCalValueDown = false;
                FatherNode.valueUp = new ConstInt(FatherNode.valueIntUp);
            } else { // 在进行局部变量初始化,没法确定初始值是否可以直接求值,所以用一个 value 代替
                exp.buildIrTree();
            }
        } else { // 在进行数组初始化
            ArrayList<value> flattenArray = new ArrayList<>();
            if (dims.size() == 1) {  // 一维数组
                for (InitVal initVal : initVals) {
                    if (FatherNode.globalInitDown) { // 全局变量数组初始化,这里的值一定是可以被计算出来的
                        FatherNode.canCalValueDown = true;
                        initVal.buildIrTree();
                        FatherNode.canCalValueDown = false;
                        flattenArray.add(new ConstInt(FatherNode.valueIntUp));
                    } else {
                        initVal.buildIrTree();
                        flattenArray.add(FatherNode.valueUp);
                    }
                }
            } else { // 二维数组
                for (InitVal initVal : initVals) { // 此时在遍历每个一维数组
                    initVal.setDims(new ArrayList<>(dims.subList(1, dims.size()))); // 先减少一维
                    initVal.buildIrTree();
                    flattenArray.addAll(FatherNode.valueArrayUp);
                }
            }
            FatherNode.valueArrayUp = flattenArray; // 返回
        }
    }
}
