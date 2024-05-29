package node;

import token.Token;

import java.io.PrintStream;

public class BType extends FatherNode{
    // BType -> 'int'
    private final Token bTypeToken;
    public BType(Token bTypeToken) {
        this.bTypeToken = bTypeToken;
    }

    public void output(PrintStream ps) {
        ps.print(bTypeToken.toString());
    }
}
