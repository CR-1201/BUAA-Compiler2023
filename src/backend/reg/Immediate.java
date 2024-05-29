package backend.reg;

public class Immediate extends Operand{
    private int immediate;

    public Immediate(int immediate) {
        this.immediate = immediate;
    }

    public void setImmediate(int immediate) {
        this.immediate = immediate;
    }

    public int getImmediate() {
        return immediate;
    }

    @Override
    public String toString() {
        return String.valueOf(immediate);
    }
}
