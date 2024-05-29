package Pass;

import ir.BasicBlock;
import ir.Function;
import ir.module;
import ir.value;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Stack;

/**
 * 这个 pass 十分丑陋
 * 是因为计算支配树的需求时时刻刻都存在,有的时候只对特定的函数
 * 所以无法每次都是对所有函数都遍历
 */
public class DomInfo implements Pass{
    @Override
    public void pass() {
        module m = module.getModule();
        for (value funcNode : m.getFunctions()) {
            Function func = (Function) funcNode;
            resetDomInfo(func);
        }
    }
    public static void resetDomInfo(Function func) {
        if (!func.getIsBuiltIn()) {
            computeDominanceInfo(func);
            computeDominanceFrontier(func);
        }
    }
    /**
     * 计算支配信息
     * @param function 待分析的函数
     */
    public static void computeDominanceInfo(Function function) {
        BasicBlock entry = (BasicBlock) function.getBasicBlocks().getFirst(); // entry 入口块
        // blockNum 是基本块的数目
        int blockNum = function.getBasicBlocks().size();
        // domers 是一个 bitSet 的数组,也就是说,每个基本块都有一个 bitSet,用于表示这个块的 domer（支配者）
        ArrayList<BitSet> domers = new ArrayList<>(blockNum);
        // 获得一个块列表,在初始化的时候,会被变成一个基本块列表,我们之后操作这个,因为链表操作起来不太方便
        ArrayList<BasicBlock> blockArray = new ArrayList<>();
        int index = 0; // 作为 block 的索引
        for (value basicBlockNode : function.getBasicBlocks()) { // clear existing dominance information and initialize
            BasicBlock curBlock = (BasicBlock)basicBlockNode; // 当前块
            curBlock.getDominators().clear(); // 清除原有信息
            curBlock.getI_Dominators().clear();

            blockArray.add(curBlock); // 登记数组,登记支配者
            domers.add(new BitSet());
            if (curBlock == entry) {  // 如果是入口块
                domers.get(index).set(index); // 说的就是入口块自己被自己支配
            } else {
                domers.get(index).set(0, blockNum); // 从 0 ~ numNode - 1 全部置 1
            }
            index++;
        }
        boolean changed = true;
        while (changed) { // calculate domer  不动点算法
            changed = false;index = 0;
            for (value node : function.getBasicBlocks()) { // 遍历基本块
                BasicBlock curBlock = (BasicBlock) node;
                if (curBlock != entry) { // 入口块
                    BitSet temp = new BitSet();
                    temp.set(0, blockNum); // 先全部置 1
                    // 就是下面的公式  temp <- {index} \cup (\BigCap_{j \in preds(index)} domer(j) )
                    for (BasicBlock preBlock : curBlock.getPrecursors()) {
                        int preIndex = blockArray.indexOf(preBlock);
                        temp.and(domers.get(preIndex));
                    }
                    temp.set(index); // 自己也是自己的 domer
                    if (!temp.equals(domers.get(index))) { // 将 temp 赋给 domer
                        domers.get(index).clear(); // replace domers[index] with temp
                        domers.get(index).or(temp);
                        changed = true;
                    }
                }
                index++;
            }
        }

        for (int i = 0; i < blockNum; i++) { // 在这个循环里，将 domer 信息存入基本块中
            BasicBlock curBlock = blockArray.get(i);
            BitSet domerInfo = domers.get(i);

            for (int domerIndex = domerInfo.nextSetBit(0); domerIndex >= 0;
                 domerIndex = domerInfo.nextSetBit(domerIndex + 1)) { // 这个叫做遍历每一个支配者
                BasicBlock domerBlock = blockArray.get(domerIndex);
                curBlock.getDominators().add(domerBlock); // 添加支配者
            }
        }

        for (int i = 0; i < blockNum; i++) { // calculate doms and idom
            BasicBlock curBlock = blockArray.get(i);
            for (BasicBlock maybeIdomerbb : curBlock.getDominators()) { // 遍历所有的支配者
                if (maybeIdomerbb != curBlock) { // 排除自身
                    boolean isIdom = true;
                    for (BasicBlock domerbb : curBlock.getDominators()) {
                        if (domerbb != curBlock && domerbb != maybeIdomerbb && domerbb.getDominators()
                                .contains(maybeIdomerbb)) { // 最后一个条件说明并不直接
                            isIdom = false;
                            break;
                        }
                    }
                    if (isIdom) { // 说明是直接支配点
                        curBlock.setIDirectDom(maybeIdomerbb); // 双方都需要登记
                        maybeIdomerbb.getI_Dominators().add(curBlock);
                        break;
                    }
                }
            }
        }
        computeDominanceLevel(entry, 0); // calculate dom level
    }
    /**
     * Compute the dominance frontier of all the basic blocks of a function.
     * @param function 当前函数
     */
    public static void computeDominanceFrontier(Function function) {
        for (value node : function.getBasicBlocks()) {  // 清空原支配边界
            BasicBlock block = (BasicBlock)node;
            block.getDominanceFrontier().clear();
        }
        for (value node :  function.getBasicBlocks()) {
            BasicBlock curBlock = (BasicBlock)node;
            for (BasicBlock succBlock : curBlock.getSuccessors()) {
                BasicBlock cur = curBlock; // cur 是一个游标,会顺着直接支配者链（也就是支配者树）滑动
                while (cur == succBlock || !succBlock.getDominators().contains(cur)) { // 后继块就是 cur 或者是 succBlock 的支配者不包括 cur
                    cur.getDominanceFrontier().add(succBlock);
                    // 获得直接支配者,这里说的是,如果 curBlock 的后继不受到 curBlock 的支配,那么 curBlock 的直接支配者的边界也是它
                    cur = cur.getDirectDom();
                }
            }
        }
    }
    /**
     * 通过一个 DFS,获得支配树深度
     * 支配树由直接支配关系组成
     * @param bb 基本块
     * @param domLevel 当前深度
     */
    public static void computeDominanceLevel(BasicBlock bb, Integer domLevel) {
        bb.setDomLevel(domLevel);
        for (BasicBlock succ : bb.getI_Dominators()) {
            computeDominanceLevel(succ, domLevel + 1);
        }
    }
    /**
     * 这个方法会获得支配树的后序遍历序列
     * @param func 待分析函数
     */
    public static ArrayList<BasicBlock> computeDominanceTreePostOder(Function func) {
        // 后序序列
        ArrayList<BasicBlock> postOder = new ArrayList<>();
        HashSet<BasicBlock> hasAddedSuccessor = new HashSet<>(); // 如果后继全部加进去了,那么就是 true,只有这样,才可以开始访问当前节点
        Stack<BasicBlock> stack = new Stack<>();
        stack.add(func.getFirstBlock()); // 这是因为头块一定也是支配树的根节点
        while (!stack.isEmpty()) { // 栈式 dfs
            BasicBlock parent = stack.peek();
            if (hasAddedSuccessor.contains(parent)) { // 子节点被遍历完成
                postOder.add(parent); // 那么就加入结果
                stack.pop();
                continue;
            }
            for (BasicBlock i_dominator : parent.getI_Dominators()) { // 遍历 idomee
                stack.push(i_dominator);
            }
            hasAddedSuccessor.add(parent); // 子节点已经全部入栈,表示已经遍历完成了
        }
        return postOder;
    }

}
