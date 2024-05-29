package symbol;

import node.ConstInitVal;
import node.FuncFParam;
import node.FuncType;
import node.InitVal;

import java.util.List;

public class Symbol {
    public enum Type {
        void_fuc, int_func, var, oneDimArray, twoDimArray,
    }
    public final String name;
    public ConstInitVal constInitVal;
    public InitVal initVal;
    public boolean isConst;
    public final int lineNum;
    public int paramsNum;

    public Type type;
    public FuncType returnType;
    public List<FuncFParam> funcFParams;

    @Override
    public String toString() {
        return "[Symbol] " + "name: " + name + ", isConst: " + isConst + ", lineNum: " + lineNum + ", type=" + type + ", funcFParams num=" + funcFParams.size() + "\n";
    }

    public Symbol(String name, int lineNum) {
        this.name = name;
        this.lineNum = lineNum;
        this.isConst = false;
    }

    public void setConst(boolean isConst) {
        this.isConst = isConst;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setInitVal(InitVal initVal) {
        this.initVal = initVal;
    }

    public void setConstInitVal(ConstInitVal constInitVal) {
        this.constInitVal = constInitVal;
    }

    public void setReturnType(FuncType returnType) {
        this.returnType = returnType;
    }

    public void setParamsNum(int paramsNum) {
        this.paramsNum = paramsNum;
    }

    public void setFuncFParams(List<FuncFParam> funcFParams) {
        this.funcFParams = funcFParams;
    }
}
