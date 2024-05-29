package node;

import ir.GlobalVariable;
import ir.constants.ConstArray;
import ir.constants.ConstInt;
import ir.constants.Constant;
import ir.instructions.Memory_Instrutions.ALLOCA;
import ir.irBuilder;
import ir.types.ArrayType;
import ir.types.IntType;
import ir.types.PointerType;
import ir.types.valueType;
import ir.value;
import symbol.Symbol;
import symbol.SymbolTable;
import token.Token;

import java.io.PrintStream;
import java.util.List;

public class LVal extends FatherNode{
    // LVal -> Ident {'[' Exp ']'}
    private final Token ident;
    private final List<Token> lbrackTKs;
    private final List<Exp> exps;
    private final List<Token> rbrackTKs;
    public LVal(Token ident, List<Token> lbrackTKs, List<Exp> exps, List<Token> rbrackTKs) {
        this.ident = ident;
        this.lbrackTKs = lbrackTKs;
        this.exps = exps;
        this.rbrackTKs = rbrackTKs;
        if(exps != null)childrenNode.addAll(exps);
    }

    public void output(PrintStream ps) {
        ps.print(ident.toString());
        for( int i = 0 ; i < lbrackTKs.size() ; i++ ){
            ps.print(lbrackTKs.get(i).toString());
            exps.get(i).output(ps);
            ps.print(rbrackTKs.get(i).toString());
        }
        ps.println("<LVal>");
    }

    public Token getIdent() {
        return ident;
    }

    public List<Exp> getExps() {
        return exps;
    }

    public Symbol.Type getType(SymbolTable symbolTable) {
        if(symbolTable.getType(ident) == Symbol.Type.twoDimArray){
//            System.out.println(ident.getContent()+" :2");
            if(exps.size() == 0) return Symbol.Type.twoDimArray;
            else if(exps.size() == 1) return Symbol.Type.oneDimArray;
        }
        else if(symbolTable.getType(ident) == Symbol.Type.oneDimArray){
//            System.out.println(ident.getContent()+" :1");
            if(exps.size() == 0) return Symbol.Type.oneDimArray;
        }
//        System.out.println(ident.getContent()+" :0");
        return Symbol.Type.var;
    }

    /**
     * 左值是直接返回指针的,而不是返回指针指向的内容
     * 应当由更高层次的语法树（PrimaryExpNode）决定是否加载
     * 左值指向的内容有 3 种类型:
     * 整型: 十分显然
     * 指针: 至于为什么会有这么个东西,可以这样举例,比如说 f(int a[])
     * 当我们对 a 进行 buildIr 的时候, a 的类型是 i32*
     * 然后我们为了 SSA（主要是为了整型形参,指针形参属于受害者） ,所以在函数一开始做了一个 alloca-store 操作
     * 那么在之后,我们看 a,就变成了一个 (i32*)*,也就是 lVal 指向一个指针的情况
     * 对于这种情况,我们首先用 load 将其指针去掉一层,目前 a 的类型就和 C 语言一致了,所以对它的一维访存,就是 GEP一个 index
     * 对于二维访存,就是 GEP 两个 index
     * 数组: 后面有写
     */
    @Override
    public void buildIrTree() {
        String name = ident.getContent();
//        System.out.println(FatherNode.irSymbolTable);
        value lVal = FatherNode.irSymbolTable.searchValue(name);
        if (lVal.getValueType() instanceof IntType) { // 这说明 lVal 是一个常量,直接返回就好了
            if (FatherNode.canCalValueDown) {
                FatherNode.valueIntUp = ((ConstInt) lVal).getValue();
            }
            FatherNode.valueUp = lVal;
        } else { // lVal 的类型是一个 PointerType,说明 lVal 指向的是一个局部变量或者全局变量
            valueType type = ((PointerType) lVal.getValueType()).getPointeeType(); //  lVal 指向的数据类型
            boolean isInt =  type instanceof IntType;  // 三个 boolean 指示了全局变量或者局部变量的类型
            boolean isPointer = type instanceof PointerType;
            boolean isArray = type instanceof ArrayType;
            if (isInt) { // eg :  全局变量 b = c;
                if (FatherNode.canCalValueDown && lVal instanceof GlobalVariable) { // 如果是全局变量的指针,那么在可以计算的情况下,就需要直接把这个量访存出来
                    ConstInt initVal = (ConstInt) ((GlobalVariable) lVal).getInitVal();
                    FatherNode.valueIntUp = initVal.getValue();
                    FatherNode.valueUp = new ConstInt(FatherNode.valueIntUp);
                } else { // eg :  int a = 5;  f(a);  a = 6;
                    FatherNode.valueUp = lVal; // 可以看到,左值是直接返回指针的,而不是返回指针指向的内容,应当由更高层次的语法树决定是否加载
                }
            } else if (isPointer) { // 指向的局部变量是一个指针  eg :  f(a[]) 也就是 f( i32* %0 )在 builder 形参的时候,会出现指针指向指针的情况
//                System.out.println("isPointer :"+lVal);
                value ptr = builder.buildLOAD(FatherNode.curBlock, lVal);  // 这里存着实际的指针
                if (exps.isEmpty()) { // 没有索引, 因为 Sysy 中没有指针运算, 只能被用当成子函数的实参
                    FatherNode.valueUp = ptr;
                } else if (exps.size() == 1) { // 只有一维索引,对于 a[2] 可能作为函数 f(b) 的实参 或 int a[2] = { 1, 2 }; a[2] = 1;
                    exps.get(0).buildIrTree();
                    ptr = builder.buildGEP(FatherNode.curBlock, ptr, valueUp); // 根据索引获得一个指针,要维持原有指针的类型
                    // 进行一个降维操作,这是因为此时对应的情况是 对于 int a[4][5] = ...; a[2] 可能作为函数 f(b[]) 的实参
                    // 如果对于二维数组,只进行一维访存,说明这个东西用来作为实参,那么这个实参一定是一维的,而这样需要再次降维
                    if (((PointerType) ptr.getValueType()).getPointeeType() instanceof ArrayType) {
                        ptr = builder.buildGEP(FatherNode.curBlock, ptr, ConstInt.ZERO, ConstInt.ZERO);
                    }
                    FatherNode.valueUp = ptr;
                } else { // 有两维索引 a[1][2] 只能作为 f(b) 的实参 或  赋值语句 a[1][2] = 5 返回指针,由更高层次的语法树节点决定是否 load
                    exps.get(0).buildIrTree();
                    value firstIndex = FatherNode.valueUp;
                    exps.get(1).buildIrTree();
                    value secondIndex = FatherNode.valueUp;
                    valueUp = builder.buildGEP(curBlock, ptr, firstIndex, secondIndex);
                }
            } else if (isArray) { // 是一个局部数组或者全局数组
                if (FatherNode.canCalValueDown && lVal instanceof GlobalVariable) { // 当可计算且是一个全局变量的时候, 直接将其算出来, 而不用 GEP 去做,虽然 GEP 提供了更加统一的观点对待 Alloca 数组和 global 数组
                    Constant initVal = ((GlobalVariable) lVal).getInitVal(); // 但是 GEP 在全局变量被用在 “全局” 和 其他局部数组的 Alloca 时无能为力
                    for (Exp exp : exps) {
                        exp.buildIrTree();
                        initVal = ((ConstArray) initVal).getElementByIndex(FatherNode.valueIntUp);
                    }
                    FatherNode.valueIntUp = ((ConstInt) initVal).getValue();
                } else if (FatherNode.canCalValueDown && lVal instanceof ALLOCA) { // 对于局部常量数组的常量式访问（比如说 用于初始化其他常量,用来当某数组的维度等, 我们不用 gep 访存）
                    Constant initVal = ((ALLOCA) lVal).getInitVal(); // 值得注意的是 局部常量数组存储在了 Alloca 内
                    for (Exp exp : exps) {
                        exp.buildIrTree();
                        initVal = ((ConstArray) initVal).getElementByIndex(FatherNode.valueIntUp);
                    }
                    assert initVal instanceof ConstInt;
                    FatherNode.valueIntUp = ((ConstInt) initVal).getValue();
                } else {
                    value ptr = lVal;
                    for (Exp exp : exps) {
                        exp.buildIrTree();
                        ptr = builder.buildGEP(FatherNode.curBlock, ptr, ConstInt.ZERO, FatherNode.valueUp);
                    }
                    // 当一个数组符号经过了中括号的运算后,依然指向一个数组,那么说明这个 lVal 一定是指针实参
                    // 否则如果整型实参,这里一定指向的是 INT,但是由于 llvm ir 的数组的指针是高一级的,比如说
                    // int a[2] 在 C 中,a 是指向 int 的指针,而在 llvm ir 中是指向 2 x int 的指针,所以要降级
                    // 至于为啥要降级, 对于 int a[4][5] = ...; a[2] 可能作为函数 f(b[]) 的实参
                    if (((PointerType) ptr.getValueType()).getPointeeType() instanceof ArrayType) {
                        ptr = builder.buildGEP(FatherNode.curBlock, ptr, ConstInt.ZERO, ConstInt.ZERO);
                    }
                    FatherNode.valueUp = ptr;
                }
            }
        }
    }
}
