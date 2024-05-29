package node;

import symbol.Symbol;
import symbol.SymbolTable;

import java.io.PrintStream;

public class Exp extends FatherNode{
    // Exp -> AddExp

    private final AddExp addExp;
    public Exp(AddExp addExp) {
        this.addExp = addExp;
        childrenNode.add(addExp);
    }

    public void output(PrintStream ps){
        addExp.output(ps);
        ps.println("<Exp>");
    }

    public AddExp getAddExp() {
        return addExp;
    }

    public Symbol.Type getType(SymbolTable symbolTable) {
        return addExp.getType(symbolTable);
    }
}
