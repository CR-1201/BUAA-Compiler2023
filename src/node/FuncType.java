package node;

import token.Token;

import java.io.PrintStream;

public class FuncType extends FatherNode{
    // FuncType -> 'void' | 'int'
    private final Token funcType;
    public FuncType(Token funcType) {
        this.funcType = funcType;
    }

    public void output(PrintStream ps) {
        ps.print(funcType.toString());
        ps.println("<FuncType>");
    }

    public Token getType() {
//        if(funcType.getContent() == "int"){
//            return Type.INT;
//        }
//        else return Type.VOID;
        return funcType;
    }
}
