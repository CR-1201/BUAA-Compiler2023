package backend.mipsInstruction;

import backend.reg.Immediate;
import backend.reg.Operand;
/**
 * 包括所有的使用到两个操作数的计算指令,乘除法也在这儿
 */
public class Binary extends Instruction{

    private final String type;
    private Operand dst;
    private Operand op1;
    private Operand op2;

    public Binary(String type, Operand dst, Operand op1, Operand op2) {
        this.type = type;
        setDst(dst);setOp1(op1);setOp2(op2);
    }

    public void setDst(Operand dst) {
        addDefReg(this.dst, dst);
        this.dst = dst;
    }

    public void setOp1(Operand op1) {
        addUseReg(this.op1, op1);
        this.op1 = op1;
    }

    public void setOp2(Operand op2) {
        addUseReg(this.op2, op2);
        this.op2 = op2;
    }

    public static Binary Addu(Operand dst, Operand op1, Operand op2) {
        return new Binary("addu", dst, op1, op2);
    }
    public static Binary Subu(Operand dst, Operand op1, Operand op2) {
        return new Binary("subu", dst, op1, op2);
    }
    public static Binary Xor(Operand dst, Operand op1, Operand op2) {
        return new Binary("xor", dst, op1, op2);
    }
    public static Binary Sltu(Operand dst, Operand op1, Operand op2) {
        return new Binary("sltu", dst, op1, op2);
    }
    public static Binary Slt(Operand dst, Operand op1, Operand op2) {
        return new Binary("slt", dst, op1, op2);
    }
    public static Binary Mul(Operand dst, Operand op1, Operand op2) {
        return new Binary("mul", dst, op1, op2);
    }
    public static Binary SmMul(Operand dst, Operand op1, Operand op2) {
        return new Binary("smMul", dst, op1, op2);
    }
    public static Binary Div(Operand dst, Operand op1, Operand op2) {
        return new Binary("div", dst, op1, op2);
    }
    public static Binary SmMadd(Operand dst, Operand op1, Operand op2) {
        return new Binary("smMadd", dst, op1, op2);
    }

    public String getType() {
        return type;
    }

    public Operand getDst() {
        return dst;
    }

    public Operand getOp1() {
        return op1;
    }

    public Operand getOp2() {
        return op2;
    }

    public boolean isOp2Imm() {
        return op2 instanceof Immediate;
    }

    @Override
    public void replaceReg(Operand oldReg, Operand newReg) {
        if (dst.equals(oldReg)) setDst(newReg);
        if (op1.equals(oldReg)) setOp1(newReg);
        if (op2.equals(oldReg)) setOp2(newReg);
    }
    @Override
    public void replaceUseReg(Operand oldReg, Operand newReg) {
        if (op1.equals(oldReg)) setOp1(newReg);
        if (op2.equals(oldReg)) setOp2(newReg);
    }

    /**
     * 区分是否是 imm 指令
     * 另外乘除法应该也是需要区分的
     * @return 指令字符串
     */
    @Override
    public String toString() {
        if (isOp2Imm()) {
            if(((Immediate)op2).getImmediate() == 0 && dst.equals(op1)){
                if(type.equals("addu") || type.equals("subu"))return "";
            }
            return switch (type) { // 之所以没有 subiu 是因为这条指令是拓展指令
                case "addu" -> "addiu " + dst + ", " + op1 + ", " + op2 + "\n";
                case "subu" -> "subiu " + dst + ", " + op1 + ", " + op2 + "\n";
                case "sltu" -> "sltiu " + dst + ", " + op1 + ", " + op2 + "\n";
                default -> type + "i " + dst + ", " + op1 + ", " + op2 + "\n";
            };
        } else {
            return switch (type) {
                case "smMul" -> "mult " + op1 + ", " + op2 + "\n " + "\tmfhi " + dst + "\n";
                case "div" -> "div " + op1 + ", " + op2 + "\n " + "\tmflo " + dst + "\n";
                case "smMadd" -> "madd " + op1 + ", " + op2 + "\n " + "\tmfhi " + dst + "\n";
                default -> type + " " + dst + ", " + op1 + ", " + op2 + "\n";
            };
        }
    }
}
