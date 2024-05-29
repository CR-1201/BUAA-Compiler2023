package node;

import token.Token;

import java.io.PrintStream;
import java.util.List;

public class ConstDecl extends FatherNode{
    // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
    private final Token constToken;
    private final BType bType;
    private final List<ConstDef> constDefs;
    private final List<Token> commas;
    private final Token semicnToken;
    public ConstDecl(Token constToken, BType bType, List<ConstDef> constDef, List<Token> commas, Token semicnToken) {
        this.constToken = constToken;
        this.bType = bType;
        this.constDefs = constDef;
        this.commas = commas;
        this.semicnToken = semicnToken;
        childrenNode.add(bType);
        childrenNode.addAll(constDefs);
    }

    public void output(PrintStream ps) {
        ps.print(constToken.toString());
        bType.output(ps);
        constDefs.get(0).output(ps);
        for (int i = 1; i < constDefs.size(); i++) {
            ps.print(commas.get(i - 1).toString());
            constDefs.get(i).output(ps);
        }
        ps.print(semicnToken.toString());
        ps.println("<ConstDecl>");
    }

    public List<ConstDef> getConstDefs() {
        return constDefs;
    }
}
