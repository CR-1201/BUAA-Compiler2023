package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class irSymbolTable {
    private final ArrayList<HashMap<String, value>> symbolTable;
    /**
     * 区分 function 和 block 的行为
     */
    private boolean preEnter;
    /**
     * 初始化符号表,并且加入 global layer
     */
    public irSymbolTable(){
        this.symbolTable = new ArrayList<>();
        this.symbolTable.add(new HashMap<>());
        this.preEnter = false;
    }

    /**
     * 返回栈顶符号域
     * @return 栈顶符号域
     */
    public HashMap<String, value> getTopLayer() {
        return symbolTable.get(symbolTable.size() - 1);
    }
    /**
     * 从栈顶到栈底根据 name 查找 value
     * @param ident 标识符
     * @return value，如果没有找到，则返回 null
     */
    public value searchValue(String ident){
        for (int i = symbolTable.size() - 1; i >= 0; i--){
            if (symbolTable.get(i).containsKey(ident)){
                return symbolTable.get(i).get(ident);
            }
        }
        return null;
    }
    /**
     * 在符号表中登记 Value
     * @param ident 标识符
     * @param value irValue
     */
    public void addValue(String ident, value value){
        getTopLayer().put(ident, value);
    }
    public void pushFuncLayer(){
        symbolTable.add(new HashMap<>());
        this.preEnter = true;
    }
    public void popFuncLayer(){
        symbolTable.remove(symbolTable.size() - 1);
        this.preEnter = false;
    }
    public void pushBlockLayer(){
        if(preEnter){
            preEnter = false;
        }else{
            symbolTable.add(new HashMap<>());
        }
    }

    public void popBlockLayer(){
        if (symbolTable.size() > 2){ // 第 1 层是 global,第 2 层是 function,之后才是 block 层
            symbolTable.remove(symbolTable.size() - 1);
        }
    }
    /**
     * 第一个 layer 就是 global, 所以可以借此判断 layer 的状态
     * @return true 则 global
     */
    public boolean isGlobalLayer(){
        return symbolTable.size() == 1;
    }

    @Override
    public String toString(){
        StringBuilder s = new StringBuilder();
        for (int i = symbolTable.size() - 1; i >= 0; i--) {
            HashMap<String, value> layer = symbolTable.get(i);
            for (Map.Entry<String, value> entry : layer.entrySet()) {
                s.append(entry.getKey()).append(": ").append(entry.getValue()).append(", ");
            }
            s.append("\n======================================================\n");
        }
        return s.toString();
    }

}
