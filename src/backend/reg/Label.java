package backend.reg;

import java.util.Objects;

public class Label extends Operand{
    private final String name;

    public Label(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Label objLabel = (Label) object;
        return Objects.equals(name, objLabel.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
