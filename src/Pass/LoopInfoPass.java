package Pass;

import ir.Function;
import ir.module;
import ir.value;

public class LoopInfoPass implements Pass{
    @Override
    public void pass() {
        for (value func : module.getModule().getFunctions()) {
            ((Function)func).analyzeLoop();
        }
    }
}
