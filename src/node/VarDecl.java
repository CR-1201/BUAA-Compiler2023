package node;

import token.Token;

import java.io.PrintStream;
import java.util.List;

public class VarDecl extends FatherNode{
    // VarDecl -> BType VarDef { ',' VarDef } ';'
    private final BType bType;
    private final List<VarDef> varDefs;
    private final List<Token> commas;
    private final Token semicn;
    public VarDecl(BType bType, List<VarDef> varDefs, List<Token> commas, Token semicn) {
        this.bType = bType;
        this.varDefs = varDefs;
        this.commas = commas;
        this.semicn = semicn;
        childrenNode.add(bType);
        childrenNode.addAll(varDefs);
    }

    public void output(PrintStream ps) {
        bType.output(ps);
        varDefs.get(0).output(ps);
        for( int i = 0 ; i < commas.size() ; i++ ){
            ps.print(commas.get(i).toString());
            varDefs.get(i+1).output(ps);
        }
        ps.print(semicn.toString());
        ps.println("<VarDecl>");
    }

    public List<VarDef> getVarDefs() {
        return varDefs;
    }
}
