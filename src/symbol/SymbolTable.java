package symbol;

import node.ConstInitVal;
import node.FuncFParam;
import node.FuncType;
import node.InitVal;
import token.Token;

import java.util.ArrayList;
import java.util.List;

public class SymbolTable {
    public ArrayList<Symbol> symbolList;
    public SymbolTable father;
    public ArrayList<SymbolTable> children;
    public SymbolTable(SymbolTable father) {
        this.symbolList = new ArrayList<>();
        this.children = new ArrayList<>();
        this.father = father;
    }

    public SymbolTable newSon() {
        SymbolTable son = new SymbolTable(this);
        this.children.add(son);
        return son;
    }
    // 加入普通变量
    public void addSymbol(Token ident, boolean isConst, ConstInitVal constInitVal, InitVal initVal, int numOfBracket) {
        Symbol symbol = new Symbol(ident.getContent(), ident.getLineNum());
        symbol.setConst(isConst);
        symbol.setConstInitVal(constInitVal);
        symbol.setInitVal(initVal);

        if (numOfBracket == 0) symbol.setType(Symbol.Type.var);
        else if (numOfBracket == 1) symbol.setType(Symbol.Type.oneDimArray);
        else if (numOfBracket == 2) symbol.setType(Symbol.Type.twoDimArray);
        else symbol.setType(Symbol.Type.var); // 维度不可能超过三维
        symbolList.add(symbol);
    }

    // 加入函数
    public void addSymbol(Token ident, FuncType returnType, int paramsNum, List<FuncFParam> funcFParams) {
        Symbol symbol = new Symbol(ident.getContent(), ident.getLineNum());
        if (returnType.getType().getContent().equals("int")) symbol.setType(Symbol.Type.int_func);
        else symbol.setType(Symbol.Type.void_fuc);
        symbol.setReturnType(returnType);
        symbol.setParamsNum(paramsNum);
        symbol.setFuncFParams(funcFParams);
        symbolList.add(symbol);
    }

    // 加入函数参数
    public void addSymbol(Token ident, int type) {
        Symbol symbol = new Symbol(ident.getContent(), ident.getLineNum());
        if (type == 0) symbol.setType(Symbol.Type.var);
        if (type == 1) symbol.setType(Symbol.Type.oneDimArray);
        if (type == 2) symbol.setType(Symbol.Type.twoDimArray);
        symbolList.add(symbol);
    }

    public SymbolTable back() {
        return this.father;
    }

    public boolean isDuplicateCurField(Token ident) {
        for (Symbol symbol : symbolList) {
            if (symbol.name.equals(ident.getContent())) {
                return true;
            }
        }
        return false;
    }

    public Symbol isExistUpField(Token ident) {
        SymbolTable curTable = this;
        while (curTable != null) {
            for (Symbol symbol : curTable.symbolList) {
                if (symbol.name.equals(ident.getContent())) {
                    return symbol;
                }
            }
            curTable = curTable.father;
        }
        return null;
    }

    public boolean checkIsConst(Token ident) {
        SymbolTable curTable = this;
        while (curTable != null) {
            for (Symbol symbol : curTable.symbolList) {
                if (symbol.name.equals(ident.getContent())) {
                    return symbol.isConst;
                }
            }
            curTable = curTable.father;
        }
        return false;
    }

    public boolean checkFatherFuncType(int size) {
        SymbolTable curTable = this.father;
        while (curTable != null) {
            int listSize = curTable.symbolList.size() - 1;
            if (listSize >= 0) {
                Symbol lastItem = curTable.symbolList.get(listSize);
                if (lastItem.type == Symbol.Type.void_fuc) return size == 0;
                else if (lastItem.type == Symbol.Type.int_func) return true;
                else curTable = curTable.father;
            } else curTable = curTable.father;
        }
        System.out.println("checkFatherFuncType Error");
        return false;
    }

    public Symbol.Type getType(Token ident) {
        SymbolTable curTable = this;
        while (curTable != null) {
            for (Symbol symbol : curTable.symbolList) {
                if (symbol.name.equals(ident.getContent())) {
                    return symbol.type;
                }
            }
            curTable = curTable.father;
        }
        System.out.println("getType Not Found");
        return Symbol.Type.var;
    }

    public void getAll(SymbolTable symbolTable) {
        for (SymbolTable s : symbolTable.children) {
            getAll(s);
        }
        System.out.println(symbolTable);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        for (Symbol symbol : symbolList) {
            res.append(symbol);
        }
        return res.toString();
    }

}
