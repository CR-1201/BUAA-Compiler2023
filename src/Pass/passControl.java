package Pass;

import java.util.ArrayList;

public class passControl {
    private final ArrayList<Pass> passes = new ArrayList<>();
    public void run(){
        passes.add(new BuildCFG());
        passes.add(new DomInfo());
        passes.add(new LoopInfoPass());
//        passes.add(new GlobalVariableLocalize());
        passes.add(new Mem2reg());
        for (Pass pass : passes) {
            pass.pass();
        }
    }
}
