package node;

import java.io.PrintStream;

public class Decl extends FatherNode{
    // Decl → ConstDecl | VarDecl
    private final ConstDecl constDecl;
    private final VarDecl varDecl;

    public Decl(ConstDecl constDecl, VarDecl varDecl) {
        this.constDecl = constDecl;
        this.varDecl = varDecl;
        if( constDecl != null ){
            childrenNode.add(constDecl);
        }else if( varDecl != null ){
            childrenNode.add(varDecl);
        }
    }

    public ConstDecl getConstDecl() {
        return constDecl;
    }

    public VarDecl getVarDecl() {
        return varDecl;
    }

    public void output(PrintStream ps) {

        if (constDecl != null) {
            constDecl.output(ps);
        } else {
            varDecl.output(ps);
        }
    }
}
