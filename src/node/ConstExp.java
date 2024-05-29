package node;

import ir.constants.ConstInt;

import java.io.PrintStream;

public class ConstExp extends FatherNode{
    // ConstExp -> AddExp
    private final AddExp addExp;
    public ConstExp(AddExp addExp) {
        this.addExp = addExp;
        childrenNode.add(addExp);
    }

    public void output(PrintStream ps) {
        addExp.output(ps);
        ps.println("<ConstExp>");
    }

    public AddExp getAddExp() {
        return addExp;
    }

    @Override
    public void buildIrTree() {
        /*
         * 因为常量表达式必须给出初始值, 所以一定是可以计算的, 即使是数组, 也存储在了 ConstArray 里, 支持编译时求值
         * 所有的 ConstExp 一定是个 value 返回,而不是 valueInt 返回
         */
        FatherNode.canCalValueDown = true;
        addExp.buildIrTree();
        FatherNode.canCalValueDown = false;
        FatherNode.valueUp = new ConstInt(FatherNode.valueIntUp);
    }
}
