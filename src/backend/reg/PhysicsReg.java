package backend.reg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public class PhysicsReg extends Reg{
    private final static HashMap<Integer, String> num2reg = new HashMap<>();
    private final static HashMap<String, Integer> reg2num = new HashMap<>();
    public final static HashSet<Integer> calleeSavedReg= new HashSet<>(); // 需要被调用者保存的寄存器堆
    public final static HashSet<Integer> canAllocateReg = new HashSet<>(); // 可以被申请的寄存器堆
    static {
        // 只能读,不能写,不能用于分配
        num2reg.put(0, "$zero");
        // 采用了拓展指令,所以不要动这个寄存器,不能用于分配
        num2reg.put(1, "$at");
        // 用作返回值,不需要被调用者保存（因为保存就没有办法传递返回值了）,所以采用调用者保存
        // 可以用于分配,因为 v0 不会被保存,所以在一个父函数中重复调用,就会导致 v0 的值发生改变,而如果 v0 被分配了,那么就会 bug
        num2reg.put(2, "$v0");
        // 因为不存在复杂结构,所以这个可以被当成普通的被调用者保存寄存器,可以用于分配
        num2reg.put(3, "$v1");
        // 4 个传参寄存器,是调用者保存（甚至不需要保存）,可以用于分配,即使内容被改写,其父函数只会写这个寄存器,不会读这个寄存器
        num2reg.put(4, "$a0");
        num2reg.put(5, "$a1");
        num2reg.put(6, "$a2");
        num2reg.put(7, "$a3");
        // 从 8 ~ 25 是被调用者保存,可以被分配
        num2reg.put(8, "$t0");
        num2reg.put(9, "$t1");
        num2reg.put(10, "$t2");
        num2reg.put(11, "$t3");
        num2reg.put(12, "$t4");
        num2reg.put(13, "$t5");
        num2reg.put(14, "$t6");
        num2reg.put(15, "$t7");
        num2reg.put(16, "$s0");
        num2reg.put(17, "$s1");
        num2reg.put(18, "$s2");
        num2reg.put(19, "$s3");
        num2reg.put(20, "$s4");
        num2reg.put(21, "$s5");
        num2reg.put(22, "$s6");
        num2reg.put(23, "$s7");
        num2reg.put(24, "$t8");
        num2reg.put(25, "$t9");
        // 用于 OS 内核,但是 MARS 中没有,所以可以当成被调用者保存,可以被分配
        num2reg.put(26, "$k0");
        num2reg.put(27, "$k1");
        // gp, fp 同理,都可以因为功能的缺失而被当成普通寄存器,可以被分配
        // sp 是手动维护的,不需要保存,不能被分配
        num2reg.put(28, "$gp");
        num2reg.put(29, "$sp");
        num2reg.put(30, "$fp");
        // 返回地址是需要被调用者保存的,可以被分配
        num2reg.put(31, "$ra");

        for (Map.Entry<Integer, String> entry : num2reg.entrySet()) {
            reg2num.put(entry.getValue(), entry.getKey());
        }
        // 只有 zero, at, v0, a0 ~ a3, sp 不需要被调用者保存
        calleeSavedReg.add(3);
        for (int i = 8; i <= 31; i++) {
            if( i != 29 )calleeSavedReg.add(i);
        }
        // 只有 zero, at, sp 不可以
        for (int i = 2; i < 32; i++) {
            if (i != 29) canAllocateReg.add(i);
        }
    }
    public final static PhysicsReg ZERO = new PhysicsReg("$zero");
    public final static PhysicsReg AT = new PhysicsReg("$at");
    public final static PhysicsReg SP = new PhysicsReg("$sp");
    public final static PhysicsReg V0 = new PhysicsReg("$v0");
    public final static PhysicsReg RA = new PhysicsReg("$ra");
    private final int num;
    private final String reg;
    private boolean hasAllocated;

    public PhysicsReg(String reg) {
        this.reg = reg;
        this.num = reg2num.get(reg);
        this.hasAllocated = false;
    }

    public PhysicsReg(int num) {
        this.reg = num2reg.get(num);
        this.num = num;
        this.hasAllocated = false;
    }

    public PhysicsReg(int num, boolean hasAllocated) {
        this.reg = num2reg.get(num);
        this.num = num;
        this.hasAllocated = hasAllocated;
    }

    public void setAllocated(boolean hasAllocated) {
        this.hasAllocated = hasAllocated;
    }

    public int getNum() {
        return num;
    }

    /**
     * 如果一个寄存器是物理寄存器,而且还没有被分配,那么就是需要预着色的
     * 预着色是指,某些变量必须被分配到特定的寄存器
     * @return true 就是预着色
     */
    @Override
    public boolean isPrecolored(){
        return !hasAllocated;
    }

    @Override
    public boolean hasAllocated() {
        return hasAllocated;
    }

    /**
     * 对于一个物理寄存器,只要还没有被分配,那么就是需要着色的
     * @return 若未被分配,那么就是需要着色的
     */
    @Override
    public boolean needColoring() {
        return !hasAllocated;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        PhysicsReg physicsReg = (PhysicsReg) object;
        return num == physicsReg.num && hasAllocated == physicsReg.hasAllocated;
    }

    @Override
    public int hashCode() {
        return Objects.hash(num, hasAllocated);
    }

    @Override
    public String toString() {
        return reg;
    }
}
