package node;

import ir.GlobalVariable;
import ir.constants.ConstArray;
import ir.constants.ConstInt;
import ir.constants.Constant;
import ir.constants.ZeroInitializer;
import ir.instructions.Memory_Instrutions.ALLOCA;
import ir.instructions.Memory_Instrutions.GEP;
import ir.types.ArrayType;
import ir.types.IntType;
import ir.value;
import token.Token;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class VarDef extends FatherNode{
    // VarDef -> Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
    private final Token ident;
    private final List<Token> lbracks;
    private final List<ConstExp> constExps;
    private final List<Token> rbracks;
    private Token assign;
    private InitVal initVal;
    private final ArrayList<Integer> dims = new ArrayList<>();
    public VarDef(Token ident, List<Token> lbracks, List<ConstExp> constExps, List<Token> rbracks, Token assign, InitVal initVal) {
        this.ident = ident;
        this.lbracks = lbracks;
        this.constExps = constExps;
        this.rbracks = rbracks;
        this.assign = assign;
        this.initVal = initVal;
        childrenNode.addAll(constExps);
        childrenNode.add(initVal);
    }

    public VarDef(Token ident, List<Token> lbracks, List<ConstExp> constExps, List<Token> rbracks) {
        this.ident = ident;
        this.lbracks = lbracks;
        this.constExps = constExps;
        this.rbracks = rbracks;
        childrenNode.addAll(constExps);
    }

    public void output(PrintStream ps) {
        ps.print(ident.toString());
        for( int i = 0 ; i < constExps.size() ; i++ ){
            ps.print(lbracks.get(i).toString());
            constExps.get(i).output(ps);
            ps.print(rbracks.get(i).toString());
        }
        if( assign != null ){
            ps.print(assign.toString());
            initVal.output(ps);
        }
        ps.println("<VarDef>");
    }

    public Token getIdent() {
        return ident;
    }

    public List<ConstExp> getConstExps() {
        return constExps;
    }

    public InitVal getInitVal() {
        return initVal;
    }

    private void genSingleVar() {
        if (FatherNode.irSymbolTable.isGlobalLayer()) { // 全局单变量
            if (initVal != null) { // 有初始值的全局单变量
                FatherNode.globalInitDown = true;
                initVal.buildIrTree();
                FatherNode.globalInitDown = false;
                // "全局变量声明中指定的初值表达式必须是常量表达式",所以一定可以转为 ConstInt
                GlobalVariable globalVariable = builder.buildGlobalVariable(ident.getContent(), (ConstInt) FatherNode.valueUp, false);
                FatherNode.irSymbolTable.addValue(ident.getContent(), globalVariable);
            } else { // 没有初始值的全局变量
                // "未显式初始化的全局变量, 其(元素)值均被初始化为 0 "
                GlobalVariable globalVariable = builder.buildGlobalVariable(ident.getContent(), ConstInt.ZERO, false);
                FatherNode.irSymbolTable.addValue(ident.getContent(), globalVariable);
            }
        } else {  // 局部单变量
            ALLOCA alloca = builder.buildALLOCA(IntType.I32, curBlock); // 为这个变量分配空间
            // 从这里可以看出,可以从符号表这种查询到的东西是一个指针,即 int*
            FatherNode.irSymbolTable.addValue(ident.getContent(), alloca);
            if (initVal != null) { // "当不含有 '=' 和初始值时,其运行时实际初值未定义"
                initVal.buildIrTree();
                builder.buildSTORE(FatherNode.curBlock, FatherNode.valueUp, alloca);
            }
        }
    }

    /**
     * 可以根据展平的初始化数组和 dims 来生成一个合乎常理的全局变量
     * @param flattenArray 展平数组
     */
    private void genGlobalInitArray(ArrayList<value> flattenArray) {
        if (dims.size() == 1) { // 一维数组,将 flattenArray 转变后加入即可
            ArrayList<Constant> constArray = new ArrayList<>();
            for (value value : flattenArray) {
                constArray.add((ConstInt) value);
            }
            ConstArray initArray = new ConstArray(constArray);
            GlobalVariable globalVariable = builder.buildGlobalVariable(ident.getContent(), initArray, false);
            irSymbolTable.addValue(ident.getContent(), globalVariable);
        } else { // 二维数组
            ArrayList<Constant> colArray = new ArrayList<>();// 为第一维的数组,其元素为 ConstArray
            for (int i = 0; i < dims.get(0); i++) {  // 为第二维的数组,其元素为 ConstInt
                ArrayList<Constant> rowArray = new ArrayList<>();
                for (int j = 0; j < dims.get(1); j++) {
                    rowArray.add((ConstInt) flattenArray.get(dims.get(1) * i + j));
                }
                colArray.add(new ConstArray(rowArray));
            }
            ConstArray initArray = new ConstArray(colArray);
            GlobalVariable globalVariable = builder.buildGlobalVariable(ident.getContent(), initArray, false);
            irSymbolTable.addValue(ident.getContent(), globalVariable);
        }
    }
    private void genVarArray() {
        for (ConstExp constExp : constExps) { // 解析维数 exp,然后存到 dim 中
            constExp.buildIrTree();
            dims.add(((ConstInt) FatherNode.valueUp).getValue());
        }
        ArrayType arrayType = new ArrayType(IntType.I32, dims);
        if (FatherNode.irSymbolTable.isGlobalLayer()) { // 全局数组 "全局变量声明中指定的初值表达式必须是常量表达式"
            // 全局有初始值的数组
            if (initVal != null) {
                initVal.setDims(new ArrayList<>(dims));
                FatherNode.globalInitDown = true;
                initVal.buildIrTree();
                FatherNode.globalInitDown = false;
                genGlobalInitArray(FatherNode.valueArrayUp);
            } else { // 全局无初始值的数组,那么就初始化为 0
                ZeroInitializer zeroInitializer = new ZeroInitializer(arrayType);
                GlobalVariable globalVariable = builder.buildGlobalVariable(ident.getContent(), zeroInitializer, false);
                irSymbolTable.addValue(ident.getContent(), globalVariable);
            }
        } else { // 局部数组
            ALLOCA allocArray = builder.buildALLOCA(arrayType, FatherNode.curBlock); // 分配空间并登记
            irSymbolTable.addValue(ident.getContent(), allocArray);
            if (initVal != null) {  // 有初始值的局部数组
                initVal.setDims(new ArrayList<>(dims));
                initVal.buildIrTree();
                GEP basePtr = builder.buildGEP(FatherNode.curBlock, allocArray, ConstInt.ZERO, ConstInt.ZERO);
                // 如果是一个二维数组,那么就继续 GEP,上面两步之后,basePtr 会变成一个指向具体的 int 的指针,即 int*
                // 同时 basePtr 是指向 allocArray 基地址的
                if (dims.size() > 1) {
                    basePtr = builder.buildGEP(FatherNode.curBlock, basePtr, ConstInt.ZERO, ConstInt.ZERO);
                }
                for (int i = 0; i < FatherNode.valueArrayUp.size(); i++) { // 利用 store 往内存中存值
                    if (i == 0) {
                        builder.buildSTORE(FatherNode.curBlock, FatherNode.valueArrayUp.get(i), basePtr);
                    } else {
                        // 这里利用的是一维的 GEP,此时的返回值依然是 int*
                        GEP curPtr = builder.buildGEP(FatherNode.curBlock, basePtr, new ConstInt(i));
                        builder.buildSTORE(FatherNode.curBlock, FatherNode.valueArrayUp.get(i), curPtr);
                    }
                }
            }
        }
    }

    @Override
    public void buildIrTree() {
        if (constExps.isEmpty()) { // 单变量
            genSingleVar();
        } else { // 数组
            genVarArray();
        }
    }
}
