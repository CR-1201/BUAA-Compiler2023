package backend.optimizer;

import backend.mipsComponent.Block;
import backend.mipsComponent.Function;
import backend.mipsInstruction.Instruction;
import backend.reg.Reg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class BlockLiveInfo {
    private final HashSet<Reg> live_Use = new HashSet<>();
    private final HashSet<Reg> live_Def = new HashSet<>();
    private HashSet<Reg> live_In = new HashSet<>();
    private HashSet<Reg> live_Out = new HashSet<>();
    public HashSet<Reg> getLiveOut() {
        return live_Out;
    }
    /**
     * 对于每一个函数都进行一个这样的分析
     * @return block-info对 的集合 map
     */
    public static HashMap<Block, BlockLiveInfo> liveAnalysis(Function func) {
        HashMap<Block, BlockLiveInfo> liveInfoMap = new HashMap<>();
        LinkedList<Block> blocks = func.getBlocks();
        for (Block block : blocks) { // 开始遍历每一个 block
            BlockLiveInfo blockLiveInfo = new BlockLiveInfo();
            liveInfoMap.put(block, blockLiveInfo);
            for (Instruction instr : block.getInstructions()) { // 开始遍历 block 中的指令, 跟定义中的一模一样
                for(Reg reg : instr.getRegUse()){
                    if( reg.needColoring() && !blockLiveInfo.live_Def.contains(reg) ){
                        blockLiveInfo.live_Use.add(reg);
                    }
                }
                for(Reg reg : instr.getRegDef()){ // 还没使用就被定义,这里应该是错误的,因为定义就是定义,就是杀死,不会因为使用而不杀死
                    if( reg.needColoring() ){
                        blockLiveInfo.live_Def.add(reg);
                    }
                }
            }
            blockLiveInfo.live_In.addAll(blockLiveInfo.live_Use);
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Block block : blocks) {// 开始遍历 func 中的 block
                BlockLiveInfo blockLiveInfo = liveInfoMap.get(block);
                HashSet<Reg> liveOut = new HashSet<>();
                if (block.getTrueSuccessor() != null) { // 加入两个后继,LiveOut 就是 LiveIn 的并集
                    BlockLiveInfo succBlockInfo = liveInfoMap.get(block.getTrueSuccessor());
                    liveOut.addAll(succBlockInfo.live_In);
                }
                if (block.getFalseSuccessor() != null) {
                    BlockLiveInfo succBlockInfo = liveInfoMap.get(block.getFalseSuccessor());
                    liveOut.addAll(succBlockInfo.live_In);
                }
                if (!liveOut.equals(blockLiveInfo.live_Out)) { // 第一次没有办法 equal ,因为之前 liveOut 并没有被赋值
                    changed = true;
                    blockLiveInfo.live_Out = liveOut;
                    blockLiveInfo.live_In = new HashSet<>(blockLiveInfo.live_Use); // 这里模拟的是 LiveUse
                    for(Reg reg : blockLiveInfo.live_Out){
                        if(!blockLiveInfo.live_Def.contains(reg) ){ // liveIn = liveUse + liveOut - liveDef
                            blockLiveInfo.live_In.add(reg); // 这里模拟的是取差集,也是符合的,就是不知道为啥外面要加个循环
                        }
                    }
                }
            }
        }
        return liveInfoMap;
    }
}
