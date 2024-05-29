package node;

import java.io.PrintStream;

public class BlockItem extends FatherNode{
    // BlockItem -> Decl | Stmt
    private Decl decl = null;
    private Stmt stmt = null;
    public BlockItem(Decl decl) {
        this.decl = decl;
        childrenNode.add(decl);
    }
    public BlockItem(Stmt stmt) {
        this.stmt = stmt;
        childrenNode.add(stmt);
    }

    public void output(PrintStream ps) {
        if( decl == null ){
            stmt.output(ps);
        }else{
            decl.output(ps);
        }
    }

    public Stmt getStmt() {
        return stmt;
    }

    public Decl getDecl() {
        return decl;
    }
}