package Pass;

import ir.*;
import ir.constants.ConstInt;
import ir.instructions.Memory_Instrutions.ALLOCA;
import ir.instructions.Memory_Instrutions.LOAD;
import ir.instructions.Memory_Instrutions.STORE;
import ir.instructions.Other_Instructions.PHI;
import ir.instructions.instruction;
import ir.types.DataType;
import tools.Pair;

import java.util.*;

public class Mem2reg implements Pass{
    private STORE onlyStore; // 当前分析的 alloca 唯一一个 store 指令
    private BasicBlock onlyBlock; // 如果 store 和 load 在同一个块
    private BasicBlock entryBlock; // 当前函数的入口块
    private final HashSet<BasicBlock> usingBlocks = new HashSet<>(); // 对于特定的 alloca 指令, 其 load 所在的块
    private final HashSet<BasicBlock> definingBlocks = new HashSet<>(); // 对于特定的 alloca 指令, 其 store 所在的块
    private Function function;
    private final ArrayList<ALLOCA> allocas = new ArrayList<>(); // 记录当前函数可以被提升的 allocas
    private final HashMap<PHI, ALLOCA> phi2Alloca = new HashMap<>();
    @Override
    public void pass() {
            for (value func : module.getModule().getFunctions()) { // 遍历每一个函数
                this.function = (Function) func;
                process();
            }
    }
    private void process() {
        if (!function.getIsBuiltIn()) { // 内建函数不分析
            entryBlock = (BasicBlock) function.getBasicBlocks().getFirst(); // create alloca list
            for (instruction instr : entryBlock.getInstructions()) { // 检查一下是否可以优化 alloca
                if (instr instanceof ALLOCA alloca && alloca.Promotable()) {
                    allocas.add(alloca);
                }
            }
            for (value blockNode : function.getBasicBlocks()) { // 按照基本块进行 sweep(打扫)
                BasicBlock block = (BasicBlock)blockNode;
//                System.out.println(block);
                sweepBlock(block);
            }
            Iterator<ALLOCA> iterator = allocas.iterator(); // 遍历 alloca 数组
            while (iterator.hasNext()) {
                ALLOCA alloca = iterator.next();
                if (alloca.getUsers().isEmpty()) { // 之前的 sweep 会导致 alloca 的使用者减少,那么就可能出现 alloca 没有使用者的情况,此时就可以将其删除了
                    alloca.removeAllOperators();
                    alloca.eraseFromParent();
                    iterator.remove(); // 将指令从节点中移出
                    continue;
                }
                analyzeAllocaInfo(alloca); // onlyStore
                if (definingBlocks.size() == 0) { // no store,删除这个 alloca 和与之相关的 load
                    ArrayList<user> loadClone = new ArrayList<>(alloca.getUsers());
                    for (user user : loadClone) {
                        LOAD load = (LOAD) user;
                        load.selfReplace(ConstInt.ZERO);
                        load.removeAllOperators();
                        load.eraseFromParent();
                    }
                    alloca.removeAllOperators();
                    alloca.eraseFromParent();
                    iterator.remove();
                    continue;
                }
                if (onlyStore != null && dealOnlyStore(alloca)) { // onlyStore
                    iterator.remove();
                    continue;
                }
                if (onlyBlock != null) { // store / load in one block
                    dealOneBlock(alloca);
                    iterator.remove();
                    continue;
                }
                HashSet<BasicBlock> phiBlocks = calIDF(definingBlocks); // 所有 definingBlocks 的递归支配边界（递归边界的闭包）都是需要插入 phi 节点的
                phiBlocks.removeIf(block -> !isPhiAlive(alloca, block)); // 去掉不需要插入的节点
                for (BasicBlock phiBlock : phiBlocks) { // insert phi node
                    PHI phi = irBuilder.getIrBuilder().buildPHI((DataType) alloca.getAllocatedType(), phiBlock);
                    phi2Alloca.put(phi, alloca);
                }
            }
            if (allocas.isEmpty()) { // 如果 alloca 都被删除了（一般表示不需要插入 phi 就可以结束战斗）
                return;
            }
            renamePhiNode(); // rename phi node and add incoming <value, block>
            for (ALLOCA ai : allocas) {
                ai.removeAllOperators();
                ai.eraseFromParent();
            }
            allocas.clear();
            phi2Alloca.clear();
        }
    }
    /**
     * 利用不动点法求解支配边界的闭包
     * 也就是支配边界,支配边界的支配边界,支配边界的支配边界的支配边界
     * @param definingBlocks 拥有 store 的点
     * @return 支配边界的闭包
     */
    private HashSet<BasicBlock> calIDF(HashSet<BasicBlock> definingBlocks) {
        HashSet<BasicBlock> ans = new HashSet<>();
        for (BasicBlock definingBlock : definingBlocks) {
            ans.addAll(definingBlock.getDominanceFrontier());
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            HashSet<BasicBlock> newAns = new HashSet<>(ans);
            for (BasicBlock block : ans) {
                newAns.addAll(block.getDominanceFrontier());
            }
            if (newAns.size() > ans.size()) {
                changed = true;
                ans = newAns;
            }
        }
        return ans;
    }

    /**
     * 有插入 phi 的必要:也就是有与 alloca 相关的 load 或者 store
     * @param alloca alloca
     * @param block 当前块
     * @return 需要插入则为 true
     */
    private boolean isPhiAlive(ALLOCA alloca, BasicBlock block) {
        for (instruction instruction : block.getInstructions()) {
            if (instruction instanceof LOAD && instruction.getValue(0) == alloca) {
                return true;
            } else if (instruction instanceof STORE && instruction.getValue(1) == alloca) {
                return false;
            }
        }
        return true;
    }
    private void dealOneBlock(ALLOCA alloca) {
        boolean flag = false;
        for (instruction instruction : onlyBlock.getInstructions()) { // 遍历所有的指令
            if (instruction instanceof LOAD && instruction.getValue(0) == alloca && !flag) { // 遇到了还没有 store 就先 load 的情况，直接删掉
                instruction.selfReplace(ConstInt.ZERO);
                instruction.removeAllOperators();
                instruction.eraseFromParent();
            } else if (instruction instanceof STORE && instruction.getValue(1) == alloca) {
                if (flag) {
                    instruction.removeAllOperators();
                    instruction.eraseFromParent();
                } else {
                    flag = true;
                }
            }
        }
        alloca.removeAllOperators();
        alloca.eraseFromParent();
    }
    /**
     * 处理只有一个 store 的情况
     * @param alloca 当前 alloca
     * @return usingBlock 是否为空,本质上是是否需要进行下一步处理,如果是 false 那么就需要继续处理
     */
    private boolean dealOnlyStore(ALLOCA alloca) {
        usingBlocks.clear(); // construct later
        value replaceValue = onlyStore.getValue(); // replaceValue 是 store 向内存写入的值
        ArrayList<user> users = new ArrayList<>(alloca.getUsers());
        for (user user : users) { // 只有一个 store ,其他都是 load
            if (!(user instanceof STORE)) {
                LOAD load = (LOAD) user;
                // 如果 store 所在的块是 load 的支配者,那么就将用到 load 读入值的地方换成 store
                if (onlyStore.getParent() != load.getParent() && onlyStore.getParent().isDominator(load.getParent())) {
                    load.selfReplace(replaceValue);
                    load.removeAllOperators();
                    load.eraseFromParent();
                } else usingBlocks.add(load.getParent());
            }
        }
        if (usingBlocks.isEmpty()) {
            onlyStore.removeAllOperators();
            onlyStore.eraseFromParent();
            alloca.removeAllOperators();
            alloca.eraseFromParent();
        }
        return usingBlocks.isEmpty();
    }
    /**
     * definingBlocks, usingBlocks, onlyStore, onlyBlock
     * @param alloca 当前分析的 alloca
     */
    private void analyzeAllocaInfo(ALLOCA alloca) {
        definingBlocks.clear(); // 清空
        usingBlocks.clear();
        onlyBlock = null;
        onlyStore = null;
        int storeNum = 0; // 使用 alloca 的 store 的指令个数
        for (value user : alloca.getUsers()) { // 遍历使用 alloca 的指令（就是 load 和 store）
            if (user instanceof STORE store) { // 如果是 store
                definingBlocks.add(store.getParent());
                if (storeNum == 0) {
                    onlyStore = (STORE) user;
                }
                storeNum++;
            } else if (user instanceof LOAD load) {
                usingBlocks.add(load.getParent());
            }
        }
        if (storeNum > 1) {
            onlyStore = null;
        }
        if (definingBlocks.size() == 1 && definingBlocks.equals(usingBlocks)) {
            onlyBlock = definingBlocks.iterator().next();
        }
    }
    /**
     * 在一个基本块中,对于 store -> alloca -> load 这样的逻辑链条  这样的 store 和 load 一定是在对非数组读写,不然就是 store - gep - load
     * 用 store 的值代替所有的 load 出的值,然后将 load 删除
     * 之后再对同一个 alloca 的多次 store,简化为最后一次
     * @param block 当前块
     */
    private void sweepBlock(BasicBlock block) {
        HashMap<ALLOCA, STORE> alloca2store = new HashMap<>();
        LinkedList<instruction> instructions = new LinkedList<>(block.getInstructions());
        for (instruction instr : instructions) {
//            System.out.println(instr);
            if (instr instanceof STORE store && instr.getValue(1) instanceof ALLOCA alloca) {  // 如果当前指令是 store 指令,而且地址是 alloca 分配的,那么就存到 alloca2store 中
                alloca2store.put(alloca, store);
            } else if (instr instanceof LOAD && instr.getValue(0) instanceof ALLOCA alloca) { // 如果当前指令是 load,而且地址是 alloca
                STORE store = alloca2store.get(alloca);
                if (store == null && block == entryBlock) {
                    instr.selfReplace(ConstInt.ZERO);
                    instr.removeAllOperators();
                    instr.eraseFromParent();
                } else if (store != null) {
                    instr.selfReplace(store.getValue()); // 首先用 store 的要存入的值代替了 load 要读入的值
                    instr.removeAllOperators(); // 将这条 load 指令删除
                    instr.eraseFromParent();
                }
            }
        }
//        System.out.println("-----------------");
        alloca2store.clear(); // 清空对应关系
        LinkedList<instruction> instructions1 = block.getInstructions();
        for( int i = instructions1.size()-1 ; i >= 0 ; i-- ){ // 进行倒序遍历
            instruction instr = instructions1.get(i);
//            System.out.println(instr);
            if (instr instanceof STORE && instr.getValue(1) instanceof ALLOCA alloca) { // 如果是 store 指令
                STORE store = alloca2store.get(alloca);
                if (store != null) { // 这不是最后一条对于 alloca 这个内存的写,那么就删除
                    instr.removeAllOperators();
                    instr.eraseFromParent();
                } else { // 记录并加入
                    alloca2store.put(alloca, (STORE) instr);
                }
            }
        }
//        System.out.println(alloca2store);
    }
    private void renamePhiNode() {
        // 重命名,完成 phi 的嵌入
        HashMap<BasicBlock, Boolean> visitMap = new HashMap<>();
        HashMap<ALLOCA, value> variable = new HashMap<>();
        for (value block : function.getBasicBlocks()) {
            visitMap.put((BasicBlock) block, false);
        }
        for (ALLOCA alloca : allocas) {
            variable.put(alloca, ConstInt.ZERO); // default undef is 0
        }
        Stack<Pair<BasicBlock, HashMap<ALLOCA, value>>> bbStack = new Stack<>();  // 手动 dfs
        bbStack.push(new Pair<>(entryBlock, variable));
        while (!bbStack.isEmpty()) {
            Pair<BasicBlock, HashMap<ALLOCA, value>> tmp = bbStack.pop();
            BasicBlock currentBlock = tmp.getFirst();
            variable = tmp.getSecond();
            if (visitMap.get(currentBlock)) {
                continue;
            }
            int i = 0;
            ArrayList<instruction> instructions = currentBlock.getInstructionsArray(); // 遍历当前块的所有指令
            while (instructions.get(i) instanceof PHI) {
                variable.put(phi2Alloca.get((PHI) instructions.get(i)), instructions.get(i));
                i++;
            }
            while (i < instructions.size()) {
                instruction instr = instructions.get(i);
                if (instr instanceof LOAD load) {
                    if (load.getAddr() instanceof ALLOCA alloca) {
                        instr.selfReplace(variable.get(alloca));
                        instr.removeAllOperators();
                        instr.eraseFromParent();
                    }
                } else if (instr instanceof STORE store) {
                    if (store.getAddr() instanceof ALLOCA alloca) {
                        variable.put(alloca, store.getValue());
                        instr.removeAllOperators();
                        instr.eraseFromParent();
                    }
                }
                i++;
            }
            for (BasicBlock successor : currentBlock.getSuccessors()) {
                instructions = successor.getInstructionsArray();
                i = 0;
                while (instructions.get(i) instanceof PHI phi) {
                    ALLOCA ai = phi2Alloca.get(phi);
                    phi.addIncoming(variable.get(ai), currentBlock);
                    i++;
                }
                if (!visitMap.get(successor)) {
                    bbStack.push(new Pair<>(successor, new HashMap<>(variable)));
                }
            }
            visitMap.put(currentBlock, true);
        }
    }
}
