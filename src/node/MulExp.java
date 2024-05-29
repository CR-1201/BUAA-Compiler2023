package node;

import ir.constants.ConstInt;
import ir.instructions.Binary_Instructions.MUL;
import ir.instructions.Binary_Instructions.SDIV;
import ir.value;
import symbol.Symbol;
import symbol.SymbolTable;
import token.Token;

import java.io.PrintStream;
import java.util.List;

public class MulExp extends FatherNode{
    // MulExp -> UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
    private List<UnaryExp> unaryExps;
    private List<Token> mulExpTKs;
    public MulExp(List<UnaryExp> unaryExps, List<Token> mulExpTKs) {
        this.unaryExps = unaryExps;
        this.mulExpTKs = mulExpTKs;
        childrenNode.addAll(unaryExps);
    }

    public void output(PrintStream ps) {
        unaryExps.get(0).output(ps);
        ps.println("<MulExp>");
        for( int i = 0 ; i < mulExpTKs.size() ; i++ ){
            ps.print(mulExpTKs.get(i).toString());
            unaryExps.get(i+1).output(ps);
            ps.println("<MulExp>");
        }
    }

    public List<UnaryExp> getUnaryExps() {
        return unaryExps;
    }
    public List<Token> getMulExpTKs(){
        return mulExpTKs;
    }

    public Symbol.Type getType(SymbolTable symbolTable) {
        return unaryExps.get(0).getType(symbolTable);
    }

    @Override
    public void buildIrTree() {
        if (FatherNode.canCalValueDown){
            getUnaryExps().get(0).buildIrTree();
            int out = FatherNode.valueIntUp;
            for (int i = 1; i < getUnaryExps().size(); i++){
                getUnaryExps().get(i).buildIrTree();
                if ( getMulExpTKs().get(i-1).getType() == Token.tokenType.MULT ){
                    out *= FatherNode.valueIntUp;
                } else if ( getMulExpTKs().get(i-1).getType() == Token.tokenType.DIV ){
                    out /= FatherNode.valueIntUp;
                } else if ( getMulExpTKs().get(i-1).getType() == Token.tokenType.MOD ){
                    out %= FatherNode.valueIntUp;
                }
            }
            FatherNode.valueIntUp = out;
            FatherNode.valueUp = new ConstInt( FatherNode.valueIntUp );
        }else {
            getUnaryExps().get(0).buildIrTree();
            value out = FatherNode.valueUp;
            if ( out.getValueType().isI1() ){ // cast i1 value 2 i32
                out = builder.buildZEXT(FatherNode.curBlock, out);
            }
            for (int i = 1; i < getUnaryExps().size(); i++){
                getUnaryExps().get(i).buildIrTree();
                value multer = FatherNode.valueUp;
                if (multer.getValueType().isI1()){
                    multer = builder.buildZEXT(FatherNode.curBlock, multer);
                }
                if ( getMulExpTKs().get(i-1).getType() == Token.tokenType.MULT ){
                    out = builder.buildMUL(FatherNode.curBlock, out, multer);
                }else if ( getMulExpTKs().get(i-1).getType() == Token.tokenType.DIV ){
                    out = builder.buildSDIV(FatherNode.curBlock, out, multer);
                }else if ( getMulExpTKs().get(i-1).getType() == Token.tokenType.MOD ) { // x % y = x - ( x / y ) * y,这是因为取模优化不太好做
//                    System.out.println("multer: "+ multer);
                    if (multer instanceof ConstInt){
                        int num = ((ConstInt) multer).getValue();
                        if (Math.abs(num) == 1){ // 如果绝对值是 1,那么就翻译成 MOD,这就交给后端优化了
                            out = builder.buildSREM(FatherNode.curBlock, out, multer);
                        } else if ((Math.abs(num) & (Math.abs(num) - 1)) == 0){ // 如果是 2 的幂次
                            out = builder.buildSREM(FatherNode.curBlock, out, multer);
                        } else {
                            SDIV a = builder.buildSDIV(FatherNode.curBlock, out, multer);
                            MUL b = builder.buildMUL(FatherNode.curBlock, a, multer);
                            out = builder.buildSUB(FatherNode.curBlock, out, b);
                        }
                    } else {
                        SDIV a = builder.buildSDIV(FatherNode.curBlock, out, multer);
                        MUL b = builder.buildMUL(FatherNode.curBlock, a, multer);
                        out = builder.buildSUB(FatherNode.curBlock, out, b);
                    }
                }
            }
            FatherNode.valueUp = out;
        }
    }
}
