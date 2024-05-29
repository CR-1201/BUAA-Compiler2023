package node;

import ir.GlobalVariable;
import ir.constants.ConstArray;
import ir.constants.ConstInt;
import ir.constants.Constant;
import ir.instructions.Memory_Instrutions.ALLOCA;
import ir.instructions.Memory_Instrutions.GEP;
import ir.types.ArrayType;
import ir.types.IntType;
import token.Token;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class ConstDef extends FatherNode{
    // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
    private final Token identToken;
    private final List<Token> lbrackToken;
    private final List<ConstExp> constExps;
    private final List<Token> rbrackToken;
    private final Token eqlToken;
    private final ConstInitVal constInitVal;
    /*
     * 用于记录数组的维数,比如说 a[1][2] 的 dims 就是 {1, 2}
     */
    private final ArrayList<Integer> dims = new ArrayList<>();
    public ConstDef(Token identToken, List<Token> lbrackToken, List<ConstExp> constExps, List<Token> rbrackToken, Token eqlToken, ConstInitVal constInitVal) {
        this.identToken = identToken;
        this.lbrackToken = lbrackToken;
        this.constExps = constExps;
        this.rbrackToken = rbrackToken;
        this.eqlToken = eqlToken;
        this.constInitVal = constInitVal;
        childrenNode.addAll(constExps);
        childrenNode.add(constInitVal);
    }

    public ArrayList<Integer> getDims(){
        return dims;
    }

    public void output(PrintStream ps) {
        ps.print(identToken.toString());
        for( int i = 0 ; i  < constExps.size() ; i++ ){
            ps.print(lbrackToken.get(i).toString());
            constExps.get(i).output(ps);
            ps.print(rbrackToken.get(i).toString());
        }
        ps.print(eqlToken.toString());
        constInitVal.output(ps);
        ps.println("<ConstDef>");
    }

    public Token getIdent() {
        return identToken;
    }

    public List<ConstExp> getConstExps() {
        return constExps;
    }

    public ConstInitVal getConstInitVal() {
        return constInitVal;
    }

    @Override
    public void buildIrTree(){
        // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
        String ident = getIdent().getContent(); // 获得常量的名字
        if (getConstExps().isEmpty()){ // 单变量
            getConstInitVal().buildIrTree();
            // 单变量是不算 GlobalVariable 的,因为没啥意义,只放在符号表中
            // 常量在符号表中对应一个 ConstInt 值,哪怕是在局部,也是不分配栈上空间的
            FatherNode.irSymbolTable.addValue(ident, FatherNode.valueUp);
        } else{ // 常量数组
            for (ConstExp constExp : getConstExps()){ // 解析维数 exp,然后存到 dim 中
                constExp.buildIrTree();
                getDims().add(((ConstInt) FatherNode.valueUp).getValue());
            }
            // 方便对于 initValue 的分析
            getConstInitVal().setDims(getDims());
            // 分析 initValue
            FatherNode.globalInitDown = true;
            getConstInitVal().buildIrTree();
            FatherNode.globalInitDown = false;
            if (FatherNode.irSymbolTable.isGlobalLayer()){ // 如果是全局数组,那么是不需要 alloca 指令的,本质是其在静态区
                GlobalVariable globalVariable = builder.buildGlobalVariable(ident, (Constant) FatherNode.valueUp, true);  // 加入全局变量
                FatherNode.irSymbolTable.addValue(ident, globalVariable); // 登记到符号表中
            }else { // 如果是局部数组
                ArrayType arrayType = new ArrayType(IntType.I32, getDims());// 根据维数信息创建数组标签,之前不用 是因为在 constInitVal 中递归生成了
                // alloca 指令诞生了
                // alloca will be moved to first basic_block in curfunc
                // alloca 的指针就是指向这个数组的指针
                ALLOCA allocArray = builder.buildALLOCA(arrayType, FatherNode.curBlock, (ConstArray) FatherNode.valueUp);
                FatherNode.irSymbolTable.addValue(ident, allocArray);// 登记符号表
                // 获得一个指针,这个指针指向初始化数组的一个元素
                GEP basePtr = builder.buildGEP(FatherNode.curBlock, allocArray, ConstInt.ZERO, ConstInt.ZERO);
                // 如果是一个二维数组,继续 GEP, basePtr 会变成一个指向具体的 int 的指针,即 int*
                if (getDims().size() > 1){ // basePtr 是指向 allocArray 基地址的
                    basePtr = builder.buildGEP(FatherNode.curBlock, basePtr, ConstInt.ZERO, ConstInt.ZERO);
                }
                // 利用 store 往内存中存值
                for (int i = 0; i < FatherNode.valueArrayUp.size(); i++){
                    if (i == 0){
                        builder.buildSTORE(FatherNode.curBlock, FatherNode.valueArrayUp.get(i), basePtr);
                    }else{
                        // 这里利用的是一维的 GEP,此时的返回值依然是 int*
                        GEP curPtr = builder.buildGEP(FatherNode.curBlock, basePtr, new ConstInt(i)); // 变长索引,依次赋值,其实可以不用 if-else
                        builder.buildSTORE(FatherNode.curBlock, FatherNode.valueArrayUp.get(i), curPtr);
                    }
                }
            }
        }
    }
}
