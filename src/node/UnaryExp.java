package node;

import ir.Function;
import ir.constants.ConstInt;
import ir.instructions.Binary_Instructions.ICMP;
import ir.types.DataType;
import ir.types.IntType;
import ir.value;
import symbol.Symbol;
import symbol.SymbolTable;
import token.Token;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class UnaryExp extends FatherNode{
    // UnaryExp -> PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
    private PrimaryExp primaryExp = null;
    private UnaryOp unaryOp = null;
    private UnaryExp unaryExp = null;
    private Token identTK = null;
    private Token lparentTK = null;
    private FuncRParams funcRParams = null;
    private Token rparentTK = null;

    public UnaryExp(PrimaryExp primaryExp) {
        this.primaryExp = primaryExp;
        childrenNode.add(primaryExp);
    }

    public UnaryExp(UnaryOp unaryOp, UnaryExp unaryExp) {
        this.unaryExp = unaryExp;
        this.unaryOp = unaryOp;
        childrenNode.add(unaryOp);
        childrenNode.add(unaryExp);
    }

    public UnaryExp(Token identTK, Token lparentTK, FuncRParams funcRParams, Token rparentTK) {
        this.identTK = identTK;
        this.lparentTK = lparentTK;
        this.funcRParams = funcRParams;
        this.rparentTK = rparentTK;
        childrenNode.add(funcRParams);
    }

    public void output(PrintStream ps) {
        if( primaryExp != null ){
            primaryExp.output(ps);
        }else if( unaryOp != null ){
            unaryOp.output(ps);
            unaryExp.output(ps);
        }else{
            ps.print(identTK.toString());
            ps.print(lparentTK.toString());
            if( funcRParams != null ){
                funcRParams.output(ps);
            }
            ps.print(rparentTK.toString());
        }
        ps.println("<UnaryExp>");
    }

    public PrimaryExp getPrimaryExp() {
        return primaryExp;
    }

    public UnaryExp getUnaryExp() {
        return unaryExp;
    }

    public UnaryOp getUnaryOp() {
        return unaryOp;
    }

    public Token getIdent() {
        return identTK;
    }

    public FuncRParams getFuncRParams() {
        return funcRParams;
    }

    public int getNumOfParam() {
        if (funcRParams != null) {
            return funcRParams.getNumOfParam();
        } else return 0;
    }

    public Symbol.Type getType(SymbolTable symbolTable) {
        if (primaryExp != null) return primaryExp.getType(symbolTable);
        else if (identTK!= null) return symbolTable.getType(identTK);
        else return unaryExp.getType(symbolTable);
    }

    @Override
    public void buildIrTree() {
        if (FatherNode.canCalValueDown) { // 可计算的情况只有 unaryExp 和 PrimaryExp 两种
            if ( getUnaryExp() != null) { // 处理符号即可
                getUnaryExp().buildIrTree();
                if ( getUnaryOp().getUnaryOp().getType() == Token.tokenType.MINU ){
                    FatherNode.valueIntUp = -FatherNode.valueIntUp;
                    FatherNode.valueUp = new ConstInt(FatherNode.valueIntUp);
                } else if ( getUnaryOp().getUnaryOp().getType() == Token.tokenType.NOT ){
                    FatherNode.valueIntUp =  FatherNode.valueIntUp == 0 ? 1 : 0;
                    FatherNode.valueUp = new ConstInt( FatherNode.valueIntUp);
                }
            } else { // primaryExp
                getPrimaryExp().buildIrTree();
            }
        } else { // 不可计算的情况
            if ( getUnaryExp() != null ) {
                getUnaryExp().buildIrTree();
                value unaryValue = FatherNode.valueUp;
                if (unaryValue.getValueType().isI1()) { // 先拓展
                    unaryValue = builder.buildZEXT(FatherNode.curBlock, unaryValue);
                }
                if ( getUnaryOp().getUnaryOp().getType() == Token.tokenType.NOT ) {
                    FatherNode.valueUp = builder.buildICMP(FatherNode.curBlock, ICMP.Condition.EQ, unaryValue, ConstInt.ZERO);
                } else if ( getUnaryOp().getUnaryOp().getType() == Token.tokenType.MINU ) {
                    FatherNode.valueUp = builder.buildSUB(FatherNode.curBlock, ConstInt.ZERO, unaryValue);
                }
            } else if( getPrimaryExp() != null ){ // primary
                 getPrimaryExp().buildIrTree();
            } else { // callee
                Function func = (Function) FatherNode.irSymbolTable.searchValue(getIdent().getContent()); // 找到函数
                ArrayList<value> argList = new ArrayList<>(); // 实参表
                if (getFuncRParams() != null) { // 如果有实参
                    List<Exp> params = getFuncRParams().getExps();
                    ArrayList<DataType> formalArgs = func.getValueType().getFormalArgs();
                    for (int i = 0; i < params.size(); i++) {
                        Exp param = params.get(i);
                        DataType argType = formalArgs.get(i);
                        FatherNode.paramNotNeedLoadDown = !(argType instanceof IntType); // 如果传参的是一个指针,那么就不需要加载
                        param.buildIrTree();
                        FatherNode.paramNotNeedLoadDown = false;
                        argList.add(FatherNode.valueUp);
                    }
                }
                FatherNode.valueUp = builder.buildCALL(FatherNode.curBlock, func, argList);
            }
        }
    }
}
