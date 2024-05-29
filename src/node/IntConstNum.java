package node;

import ir.constants.ConstInt;
import token.Token;

import java.io.PrintStream;

public class IntConstNum extends FatherNode{
    // Number -> IntConst
    private final Token num;
    public IntConstNum(Token num) {
        this.num = num;
    }

    public void output(PrintStream ps){
        ps.print(num.toString());
        ps.println("<Number>");
    }

    public Token getNum() {
        return num;
    }

    @Override
    public void buildIrTree() {
        int num = Integer.parseInt(getNum().getContent());
        if (FatherNode.canCalValueDown) {
            FatherNode.valueIntUp = num;
            FatherNode.valueUp = new ConstInt(FatherNode.valueIntUp);
        } else {
            FatherNode.valueUp = new ConstInt(num);
        }
    }
}
