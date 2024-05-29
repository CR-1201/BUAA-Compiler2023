package backend.reg;

import java.util.Objects;

public class VirtualReg extends Reg {
    private static int numCounter = 0;

    private final String name;

    public VirtualReg() {
        this.name = "vr" + numCounter++;
    }

    /**
     * 只要是虚拟寄存器,都是需要着色的
     * @return true
     */
    @Override
    public boolean needColoring() {
        return true;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        VirtualReg virtualReg = (VirtualReg) object;
        return Objects.equals(name, virtualReg.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
