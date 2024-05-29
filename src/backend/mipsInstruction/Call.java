package backend.mipsInstruction;

import backend.mipsComponent.Function;

public class Call extends Instruction{
    private final Function targetFunction;

    public Call(Function targetFunction) {
        this.targetFunction = targetFunction;
    }

    @Override
    public String toString() {
        return "jal " + targetFunction.getName() + "\n";
    }
}
