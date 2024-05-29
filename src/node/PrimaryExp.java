package node;

import ir.types.IntType;
import symbol.Symbol;
import symbol.SymbolTable;
import token.Token;

import java.io.PrintStream;

public class PrimaryExp extends FatherNode{
    // PrimaryExp -> '(' Exp ')' | LVal | Number
    private Token lparentTK;
    private Exp exp;
    private Token rparentTK;
    private LVal lVal;
    private IntConstNum intConst;
    public PrimaryExp(Token lparentTK, Exp exp, Token rparentTK) {
        this.lparentTK = lparentTK;
        this.exp = exp;
        this.rparentTK = rparentTK;
        childrenNode.add(exp);
    }

    public PrimaryExp(LVal lVal) {
        this.lVal= lVal;
        childrenNode.add(lVal);
    }

    public PrimaryExp(IntConstNum intConst) {
        this.intConst = intConst;
        childrenNode.add(intConst);
    }

    public void output(PrintStream ps){
        if( lVal != null ){
            lVal.output(ps);
        }else if( exp != null ){
            ps.print(lparentTK.toString());
            exp.output(ps);
            ps.print(rparentTK.toString());
        }else{
            intConst.output(ps);
        }
        ps.println("<PrimaryExp>");
    }

    public Exp getExp() {
        return exp;
    }

    public LVal getLVal() {
        return lVal;
    }

    public IntConstNum getIntConst() {
        return intConst;
    }

    public Symbol.Type getType(SymbolTable symbolTable) {
        if (intConst != null) return Symbol.Type.var;
        else if (lVal != null) return lVal.getType(symbolTable);
        else return exp.getType(symbolTable);
    }

    @Override
    public void buildIrTree() {
        /*
         * 如果是不可计算的,那么大概率有两种情况
         * (Exp),那么就直接继续递归即可
         * lVal,那么需要在这里完成加载（将指针指向的内容搞出来）
         */
        if ( FatherNode.canCalValueDown ) { // 如果是可以计算的,那么忽视这一层即可
            for (FatherNode fatherNode : childrenNode) {
                fatherNode.buildIrTree();
            }
            // 如果左值是一个 int 常量,那么就不用加载了
            // 现在这种情况,说明是个指针,指针一般说明是局部变量,那么此时需要加载了
            if (!(FatherNode.valueUp.getValueType() instanceof IntType)) {
                FatherNode.valueUp = builder.buildLOAD(FatherNode.curBlock, FatherNode.valueUp);
            }
        }else {
            if ( getExp() != null) { // 说明是表达式
                getExp().buildIrTree();
            } else if ( getLVal() != null) { // 说明是左值
                if ( FatherNode.paramNotNeedLoadDown ) { // 这个变量控制不要加载
                    FatherNode.paramNotNeedLoadDown = false;
                    getLVal().buildIrTree();
                } else {
                    getLVal().buildIrTree();
                    // 如果左值是一个 int 常量,那么就不用加载了
                    // 现在这种情况,说明是个指针,指针一般说明是局部变量，那么此时需要加载了
                    if (!(FatherNode.valueUp.getValueType() instanceof IntType)) {
                        FatherNode.valueUp = builder.buildLOAD(FatherNode.curBlock, FatherNode.valueUp);
                    }
                }
            } else if (getIntConst() != null) { // 说明是数字
                getIntConst().buildIrTree();
            }
        }
    }
}
