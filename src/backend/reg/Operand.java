package backend.reg;

public abstract class Operand {
    /**
     * 对于非物理寄存器,就是 false
     * 对于物理寄存器,是 !hasAllocated
     * 因为预着色的寄存器都是在 irParse 阶段分配的,此时的 hasAllocated == false,所以是预着色的
     * hasAllocated == true 的物理寄存器只会发生在着色阶段
     * @return 如题
     */
    public boolean isPrecolored() {
        return false;
    }
    /**
     * 对于物理寄存器,当其是未被分配的,是需要着色的
     * @return 当这个寄存器是物理寄存器,而且是没有被分配的（就是没有被着色的）,着色只会发生在着色环节
     */
    public boolean needColoring() {
        return false;
    }

    public boolean hasAllocated() {
        return false;
    }
}
