package node;

import token.Token;

import java.io.PrintStream;

public class UnaryOp extends FatherNode{
    // UnaryOp -> '+' | '−' | '!'
    private final Token unaryOp;
    public UnaryOp(Token unaryOp) {
        this.unaryOp = unaryOp;
    }

    public Token getUnaryOp() {
        return unaryOp;
    }

    public void output(PrintStream ps){
        ps.print(unaryOp.toString());
        ps.println("<UnaryOp>");
    }
}
