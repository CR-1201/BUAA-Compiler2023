package node;

import ir.types.ArrayType;
import ir.types.IntType;
import ir.types.PointerType;
import ir.types.valueType;
import symbol.Symbol;
import token.Token;

import java.io.PrintStream;
import java.util.List;

public class FuncFParam extends FatherNode{
    // FuncFParam -> BType Ident [ '[' ']' { '[' ConstExp ']' }]
    private final BType bType;
    private final Token ident;
    private final Token lbrack;
    private Token rbrack = null;
    private final List<Token> lbracks;
    private final List<ConstExp> constExps;
    private final List<Token> rbracks;
    public FuncFParam(BType bType, Token ident, Token lbrack, Token rbrack, List<Token> lbracks, List<ConstExp> constExps, List<Token> rbracks) {
        this.bType = bType;
        this.ident = ident;
        this.lbrack = lbrack;
        this.rbrack = rbrack;
        this.lbracks = lbracks;
        this.constExps = constExps;
        this.rbracks = rbracks;
        childrenNode.add(bType);
        if(constExps != null)childrenNode.addAll(constExps);
    }

    public void output(PrintStream ps) {
        bType.output(ps);
        ps.print(ident.toString());
        if(rbrack != null ){
            ps.print(lbrack.toString());
            ps.print(rbrack.toString());
            for( int i = 0 ; i < lbracks.size() ; i++ ){
                ps.print(lbracks.get(i).toString());
                constExps.get(i).output(ps);
                ps.print(rbracks.get(i).toString());
            }
        }
        ps.println("<FuncFParam>");
    }

    public Token getIdent() {
        return ident;
    }

    public Token getLbrack() {
        return lbrack;
    }
    public List<Token> getLbracks() {
        return lbracks;
    }

    public int getVarType() {
        return (lbrack == null ? 0 : 1) + lbracks.size();
    }

    public boolean checkType(Symbol.Type type){
        if(lbrack == null && (type == Symbol.Type.var ||type == Symbol.Type.int_func)) return true;
        else if(lbrack != null && lbracks.size() == 0 && type == Symbol.Type.oneDimArray) return true;
        else return lbrack != null && lbracks.size() == 1 && type == Symbol.Type.twoDimArray;
    }

    @Override
    public void buildIrTree() {
        if (lbrack == null) { // 单变量
            FatherNode.argTypeUp = new IntType(32);
        } else { // 指针
            valueType argType = new IntType(32);
            for (int i = constExps.size() - 1; i >= 0; i--) { // 先倒序遍历（其实应该最多只有一个）
                FatherNode.canCalValueDown = true;
                constExps.get(i).buildIrTree();
                FatherNode.canCalValueDown = false;
                argType = new ArrayType(argType, FatherNode.valueIntUp);
            }
            argType = new PointerType(argType); // 最终做一个指针,和 C 语言逻辑一模一样
            FatherNode.argTypeUp = (PointerType) argType;
        }
    }
}
