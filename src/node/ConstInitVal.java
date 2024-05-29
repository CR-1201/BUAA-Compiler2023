package node;

import ir.constants.ConstArray;
import ir.constants.ConstInt;
import ir.constants.Constant;
import ir.value;
import token.Token;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class ConstInitVal extends FatherNode{
    // ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
    private ConstExp constExp = null;
    private Token lbrace = null;
    private List<ConstInitVal> constInitVals = null;
    private Token rbrace = null;
    private List<Token> commas = null;
    private final ArrayList<Integer> dims = new ArrayList<>();
    public void setDims(ArrayList<Integer> dims){
        this.dims.addAll(dims);
    }
    public ArrayList<Integer> getDims(){
        return dims;
    }
    public ConstInitVal(ConstExp constExp) {
        this.constExp = constExp;
        childrenNode.add(constExp);
    }

    public ConstInitVal(Token lbrace, List<ConstInitVal> constInitVals, Token rbrace, List<Token> commas) {
        this.lbrace = lbrace;
        this.constInitVals = constInitVals;
        this.rbrace = rbrace;
        this.commas = commas;
        childrenNode.addAll(constInitVals);
    }

    public void output(PrintStream ps) {
        if( constExp == null ){
            ps.print(lbrace.toString());
            if (constInitVals.size() > 0) {
                constInitVals.get(0).output(ps);
                for (int i = 1; i < constInitVals.size(); i++) {
                    ps.print(commas.get(i-1).toString());
                    constInitVals.get(i).output(ps);
                }
            }
            ps.print(rbrace.toString());
        }else{
            constExp.output(ps);
        }
        ps.println("<ConstInitVal>");
    }

    public ConstExp getConstExp() {
        return constExp;
    }

    public List<ConstInitVal> getConstInitVals() {
        return constInitVals;
    }

    @Override
    public void buildIrTree() {
        // ConstInitVal -> ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        if (getConstExp() != null){ // 单变量
           getConstExp().buildIrTree();
        }else{ // 数组
            if (FatherNode.irSymbolTable.isGlobalLayer()){ // 全局常量数组
                ArrayList<Constant> array = new ArrayList<>();
                if (getDims().size() == 1){ // 一维数组
                    for (ConstInitVal s : getConstInitVals()){
                        s.buildIrTree();
                        array.add((ConstInt) FatherNode.valueUp);
                    }
                }else{ // 二维数组
                    for (ConstInitVal s : getConstInitVals()){
                        s.setDims(new ArrayList<>(getDims().subList(1, getDims().size()))); // 去掉一维
                        s.buildIrTree();
                        array.add((ConstArray) FatherNode.valueUp);
                    }
                }
                FatherNode.valueUp = new ConstArray(array);
            }else {
                // 局部常量数组,和变量数组的初始化类似,因为局部常量数组本质上也是个局部变量数组,所以方法都一样
                // 之所以采用 flatten 的形式,是因为在 gep 的时候, flatten 的逻辑更容易处理
                // 但是考虑到局部常量数组需要存初始值常量, 所以同时也以 valueUp 的形式返回
                ArrayList<value> flattenArray = new ArrayList<>();
                ArrayList<Constant> array = new ArrayList<>();
                if (getDims().size() == 1){ // 一维数组
                    for (ConstInitVal s : getConstInitVals()){
                        s.buildIrTree();
                        flattenArray.add(FatherNode.valueUp);
                        array.add((ConstInt) FatherNode.valueUp);
                    }
                }else{ // 二维数组
                    for (ConstInitVal s : getConstInitVals()){
                        s.setDims(new ArrayList<>(getDims().subList(1, getDims().size()))); // 去掉一维
                        s.buildIrTree();
                        flattenArray.addAll(FatherNode.valueArrayUp);
                        array.add((ConstArray) FatherNode.valueUp);
                    }
                }
                FatherNode.valueArrayUp = flattenArray; // 返回
                FatherNode.valueUp = new ConstArray(array);
            }
        }
    }
}
