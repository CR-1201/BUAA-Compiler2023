package node;

import symbol.Symbol;
import symbol.SymbolTable;
import token.Token;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class FuncRParams extends FatherNode{
    // FuncRParams -> Exp { ',' Exp }
    private final List<Exp> exps;
    private final List<Token> commas;
    public FuncRParams(List<Exp> exps, List<Token> commas) {
        this.exps = exps;
        this.commas = commas;
        childrenNode.addAll(exps);
    }

    public void output(PrintStream ps){
        exps.get(0).output(ps);
        for( int i = 0 ; i < commas.size() ; i++ ){
            ps.print(commas.get(i).toString());
            exps.get(i+1).output(ps);
        }
        ps.println("<FuncRParams>");
    }

    public List<Exp> getExps() {
        return exps;
    }

    public int getNumOfParam() {
        return exps.size();
    }

    public ArrayList<Symbol.Type> getParamType(SymbolTable symbolTable){
        ArrayList<Symbol.Type> res = new ArrayList<>();
        for(Exp exp:exps){
            res.add(exp.getType(symbolTable));
        }
        return res;
    }
}
