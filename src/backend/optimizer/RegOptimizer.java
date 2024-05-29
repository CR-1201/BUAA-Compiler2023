package backend.optimizer;

import backend.mipsComponent.Block;
import backend.mipsComponent.Function;
import backend.mipsComponent.Module;
import backend.mipsInstruction.Instruction;
import backend.mipsInstruction.Load;
import backend.mipsInstruction.Move;
import backend.mipsInstruction.Store;
import backend.reg.*;
import tools.Pair;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


import static backend.optimizer.BlockLiveInfo.liveAnalysis;
import static backend.reg.PhysicsReg.SP;

public class RegOptimizer {
    private final Module mipsModule;
    private final int K = PhysicsReg.canAllocateReg.size();
    private HashMap<Block, BlockLiveInfo> liveInfoMap;
    private HashMap<Operand, HashSet<Operand>> adjList; // 根据一个节点查询与之相关的节点组
    private HashSet<Pair<Operand, Operand>> adjSet; // 边的集合
    private HashMap<Operand, Operand> alias; // 当一条传送指令 (u,v) 被合并,且 v 已经被放入 coalescedNodes 中,alias(v) = u
    private HashMap<Operand, HashSet<Move>> moveList; // 从一个节点到与该节点相关的 mov 指令之间的映射
    private HashSet<Operand> simplifyList;
    private HashSet<Operand> freezeList; // 低度数的,传送有关的节点表
    private HashSet<Operand> spillList; // 高度数的节点表
    private HashSet<Operand> spilledNodes; // 本轮中要被溢出的节点的集合
    private HashSet<Operand> coalescedNodes; // 已合并的节点的集合,比如将 u 合并到 v,那么将 u 加入这里,然后 v 加入其他集合
    private Stack<Operand> selectStack; // 包含删除的点
    private HashSet<Move> workListMoves; // 有可能合并的传送指令集合
    private HashSet<Move> activeMoves; // 还未做好合并准备的传送指令集合
    private HashSet<Instruction> coalescedMoves; // 已经合并的传送指令集合
    private HashSet<Move> constrainedMoves; // 源操作数和目标操作数冲突的传送指令集合
    private HashSet<Move> frozenMoves; // 不考虑合并的传送指令集合
    private HashMap<Operand, Integer> degree; // 节点的度
    VirtualReg vReg = null; // 新的虚拟寄存器,用来处理溢出解决时引入的新的虚拟寄存器
    Instruction firstUseNode = null; // 似乎是第一次使用新的寄存器的 store 指令,因为替换是一个 先 store,后 load 的过程
    Block firstUseBlock = null;
    Instruction lastDefNode = null; // 最后一次使用 load 的指令
    Block lastDefBlock = null;
    HashMap<Operand, Integer> loopDepths = new HashMap<>(); // 存储操作数和所在的基本块对应的循环深度
    public RegOptimizer(Module mipsModule) {
        this.mipsModule = mipsModule;
    }
    /**
     * 这个方法用于初始化一系列的数据结构,并且在 degree 中登记物理寄存器信息
     * @param function 待分析的函数
     */
    private void init(Function function) {
        liveInfoMap = liveAnalysis(function);
        adjList = new HashMap<>();
        adjSet = new HashSet<>();
        alias = new HashMap<>();
        moveList = new HashMap<>();
        spillList = new HashSet<>();
        spilledNodes = new HashSet<>();
        workListMoves = new HashSet<>();
        activeMoves = new HashSet<>();
        coalescedNodes = new HashSet<>();
        simplifyList = new HashSet<>();
        freezeList = new HashSet<>();
        selectStack = new Stack<>();
        // 下面这三个变量不一定用得到,但是 coalescedMoves 考虑删掉里面所有的 move,似乎是之前代码没有办到的
        coalescedMoves = new HashSet<>();
        frozenMoves = new HashSet<>();
        constrainedMoves = new HashSet<>();

        degree = new HashMap<>();
        for (int i = 0; i < 32; i++) { // 对于物理寄存器,需要度无限大
            degree.put(new PhysicsReg(i), Integer.MAX_VALUE);
        }
    }
    /**
     * 在冲突图上添加无向边
     * @param u 第一个节点
     * @param v 第二个节点
     */
    private void addEdge(Operand u, Operand v) {
        if (!adjSet.contains(new Pair<>(u, v)) && !u.equals(v)) { // 如果没有这条边而且这个边不是自环
            adjSet.add(new Pair<>(u, v)); // 无向边的加法
            adjSet.add(new Pair<>(v, u));
            if (!u.isPrecolored()) { // 操作条件都是没有被预着色
                adjList.putIfAbsent(u, new HashSet<>()); // 从这里看,adjList 是一个可以用节点查询所连接的所有节点的一个结构
                adjList.get(u).add(v);
                degree.compute(u, (key, value) -> value == null ? 0 : value + 1); // degree 则是用来表示节点的度的
            }
            if (!v.isPrecolored()) {
                adjList.putIfAbsent(v, new HashSet<>());
                adjList.get(v).add(u);
                degree.compute(v, (key, value) -> value == null ? 0 : value + 1);
            }
        }
    }
    /**
     * 通过逆序遍历函数中的所有指令, 生成冲突图
     * live 是每条指令的冲突变量集合
     * @param function 待分析函数
     */
    private void build(Function function) {
        LinkedList<Block> blocks = function.getBlocks();
        for (int i = blocks.size()-1 ; i >= 0 ; i-- ) { // 倒序遍历 block,确定 range 的范围
            Block block = blocks.get(i);
            HashSet<Reg> live = new HashSet<>(liveInfoMap.get(block).getLiveOut());
            LinkedList<Instruction> instructions = block.getInstructions();
            for(int j = instructions.size()-1; j >= 0 ; j-- ) {
                Instruction instr = instructions.get(j);
                ArrayList<Reg> regDef = instr.getRegDef();
                ArrayList<Reg> regUse = instr.getRegUse();
                if (instr instanceof Move mipsMove) { // 对于 mov 指令,需要特殊处理
                    Operand src = mipsMove.getSrc();
                    Operand dst = mipsMove.getDst();
                    if (src.needColoring() && dst.needColoring()) {
                        live.remove((Reg) src);
                        moveList.putIfAbsent(src, new HashSet<>());
                        moveList.get(src).add(mipsMove);
                        moveList.putIfAbsent(dst, new HashSet<>());
                        moveList.get(dst).add(mipsMove);
                        workListMoves.add(mipsMove); // 此时是有可能被合并的
                    }
                }
                regDef.stream().filter(Reg::needColoring).forEach(live::add);
                regDef.stream().filter(Reg::needColoring).forEach(d -> live.forEach(l -> addEdge(l, d)));
                for (Reg mipsReg : regDef) { // 启发式算法的依据
                    loopDepths.put(mipsReg, block.getLoopDepth() + 1);
                }
                for (Reg mipsReg : regUse) {
                    loopDepths.put(mipsReg, block.getLoopDepth() + 1);
                }
                regDef.stream().filter(Reg::needColoring).forEach(live::remove); // 删除是为了给前一个指令一个交代（倒序遍历）,说明这个指令不再存活
                regUse.stream().filter(Reg::needColoring).forEach(live::add); // 这里代表着又活了一个指令
            }
        }
    }
    /**
     * 遍历所有的节非预着色点, 把这些节点分配加入不同的 workList
     * @param function 待分析的函数
     */
    private void makeList(Function function) {
        for (VirtualReg virReg : function.getUsedVirtualRegs()) {
            if (degree.getOrDefault(virReg, 0) >= K) { // 如果度是大于 K 的,就要加入 spillList,是可能发生实际溢出的
                spillList.add(virReg);
            } else if (moveRelated(virReg)) { // 跟 mov 指令相关的操作数,加入 freezeList
                freezeList.add(virReg);
            } else { // 否则就要加到 simplifyList 中,可以进行化简的
                simplifyList.add(virReg);
            }
        }
    }
    /**
     * 判断一个寄存器是不是 mov 指令的操作数
     * @param u 节点
     * @return 如果是, 那么是 true
     */
    private boolean moveRelated(Operand u) {
        return !nodeMoves(u).isEmpty();
    }
    /**
     * 根据节点取出一些 Move 指令,必须在 activeMoves 和 workListMoves 中
     * @param u 待检测的节点
     * @return mov 的集合
     */
    private Set<Move> nodeMoves(Operand u) {
        Set<Move> result = new HashSet<>();
        Set<Move> moves = moveList.getOrDefault(u, new HashSet<>());
        for (Move move : moves) {
            if (activeMoves.contains(move) || workListMoves.contains(move)) {
                result.add(move);
            }
        }
        return result;
    }
    /**
     * 从 adjList 中取出对应的点
     * 因为是没有删除边的操作的, 所以对于一些节点, 比如已经删掉或者合并的, 就需要从这里去掉
     * @param u 一个节
     * @return 与这个节点相连的节点组
     */
    private Set<Operand> getAdjacent(Operand u) {
        Set<Operand> result = new HashSet<>();
        Set<Operand> operands = adjList.getOrDefault(u, new HashSet<>());
        for (Operand operand : operands) {
            if (!(selectStack.contains(operand) || coalescedNodes.contains(operand))) {
                result.add(operand);
            }
        }
        return result;
    }
    /**
     * 这里进行了一个节点 u 和其相连的节点将 activeMoves 删去,然后加入到 workListMoves 的操作
     * 也就是将这个节点和与其相连的 mov 节点都从“不能合并”状态转换为“能合并”状态
     * @param u 节点
     */
    private void enableMoves(Operand u) {
        opMoves(u);
        for (Operand operand : getAdjacent(u)) {
            for (Move move : nodeMoves(operand)) {
                opMoves(operand);
            }
        }
    }
    private void opMoves(Operand operand){
        for (Move move : nodeMoves(operand)) {
            if (activeMoves.contains(move)) {
                activeMoves.remove(move);
                workListMoves.add(move);
            }
        }
    }
    /**
     * 当简化一个节点的时候, 与其相连的节点都需要进行一定的改动. 最简单的就是降低度
     * 随着度的降低, 有些节点会从某个 list 移动到另一个 list
     * @param u 相连的节点
     */
    private void decreaseDegree(Operand u) {
        int d = degree.get(u);
        degree.put(u, d - 1);
        if (d == K) { // 当 d == K 时,修改后就是 K - 1,此时需要特殊处理
            enableMoves(u);
            spillList.remove(u);
            if (moveRelated(u)) {
                freezeList.add(u);
            } else {
                simplifyList.add(u);
            }
        }
    }
    /**
     * 从 simplifyList 中删除节点,然后加入到 selectStack 中
     * 与此同时,需要修改与这个节点相关的节点的度
     */
    private void simplify() {
        Operand n = simplifyList.iterator().next();
        simplifyList.remove(n); // 取出一个节点
        selectStack.push(n);
        getAdjacent(n).forEach(this::decreaseDegree); // n的邻居点的度都降低
    }
    /**
     * @param u 被合并节点
     * @return 被合并的另一个节点
     */
    private Operand getAlias(Operand u) {
        while (coalescedNodes.contains(u)) {
            u = alias.get(u);
        }
        return u;
    }
    /**
     * 将一个节点从 freezeList 移动到 simplifyList 中, 主要用于合并
     * @param u  待合并的节点
     */
    private void addList(Operand u) {
        if (!u.isPrecolored() && !moveRelated(u) && degree.getOrDefault(u, 0) < K) {
            freezeList.remove(u);
            simplifyList.add(u);
        }
    }
    /**
     * 判断 v,u 是否可以合并的. 判断方法是考虑 v 的临边关系
     * @param v 一定是虚拟寄存器
     * @param u 可能是物理寄存器
     * @return 可以合并则为 true
     */
    private boolean adjYes(Operand v, Operand u) {
        return getAdjacent(v).stream().allMatch(t -> yes(t, u));
    }
    /**
     * t 是待合并的虚拟寄存器的邻接点, r 是待合并的预着色寄存器
     * 三个条件满足一个就可以合并
     * @param t 合并的虚拟寄存器的邻接点
     * @param r 待合并的预着色寄存器
     * @return 可以合并就是 true
     */
    private boolean yes(Operand t, Operand r) {
        return degree.get(t) < K || t.isPrecolored() || adjSet.contains(new Pair<>(t, r));
    }
    /**
     * 另一种保守地判断可不可以合并的方法
     * @param u 待合并的节点 1
     * @param v 待合并的节点 2
     * @return 可以合并就是 true
     */
    private boolean conservative(Operand u, Operand v) {
        Set<Operand> uAdjacent = getAdjacent(u);
        Set<Operand> vAdjacent = getAdjacent(v);
        uAdjacent.addAll(vAdjacent);
        long count = uAdjacent.stream().filter(n -> degree.get(n) >= K).count();
        return count < K;
    }
    /**
     * 合并操作
     * @param u 待合并的节点 1
     * @param v 待合并的节点 2
     */
    private void combine(Operand u, Operand v) {
        if (freezeList.contains(v)) {  // 从原有的 workList 中移出
            freezeList.remove(v);
        } else {
            spillList.remove(v);
        }
        coalescedNodes.add(v);
        alias.put(v, u); // 相当于 alias 的 key 是虚拟寄存器,而 value 是物理寄存器
        moveList.get(u).addAll(moveList.get(v));
        for( Operand operand : getAdjacent(v)){
            addEdge(operand, u);
            decreaseDegree(operand);
        }
        if (degree.getOrDefault(u, 0) >= K && freezeList.contains(u)) {
            freezeList.remove(u);
            spillList.add(u);
        }
    }
    /**
     * 用于合并节点
     */
    private void coalesce() {
        Move mipsMove = workListMoves.iterator().next();
        Operand u = getAlias(mipsMove.getDst());
        Operand v = getAlias(mipsMove.getSrc());
        if (v.isPrecolored()) { // 如果 v 是物理寄存器,就需要交换一下
            Operand tmp = u; u = v; v = tmp;
        }
        workListMoves.remove(mipsMove);
        if (u.equals (v)) { // 进行合并
            coalescedMoves.add(mipsMove);
            addList(u);
        } else if (v.isPrecolored() || adjSet.contains(new Pair<>(u, v))) {
            constrainedMoves.add(mipsMove);
            addList(u);
            addList(v);
        } else if ((u.isPrecolored() && adjYes(v, u)) || (!u.isPrecolored() && conservative(u, v))) {
            coalescedMoves.add(mipsMove);
            combine(u, v);
            addList(u);
        } else {
            activeMoves.add(mipsMove);
        }
    }
    /**
     * 遍历每一条与 u 有关的 mov 指令,然后将这些 mov 指令从 active 和 workList 中移出
     * @param u 待冻结的节点
     */
    private void freezeMoves(Operand u) {
        for (Move mipsMove : nodeMoves(u)) {
            if (activeMoves.contains(mipsMove)) {
                activeMoves.remove(mipsMove);
            } else {
                workListMoves.remove(mipsMove);
            }
            frozenMoves.add(mipsMove);
            Operand v = getAlias(mipsMove.getDst()).equals(getAlias(u)) ? getAlias(mipsMove.getSrc()) : getAlias(mipsMove.getDst());
            if (!moveRelated(v) && degree.getOrDefault(v, 0) < K) {
                freezeList.remove(v);
                simplifyList.add(v);
            }
        }
    }
    /**
     * 当 simplify 无法进行:没有低度数的,无关 mov 的点
     * 当 coalesce 无法进行:没有符合要求可以合并的点
     * 那么进行 freeze,放弃一个低度数的 mov 的点,就可以 simplify
     */
    private void freeze() {
        Operand u = freezeList.iterator().next();
        freezeList.remove(u);
        simplifyList.add(u);
        freezeMoves(u);
    }
    /**
     * 用到了启发式算法,因为没有 loopDepth 所以只采用一个很简单的方式,调出一个需要溢出的节点,这个节点的性质是溢出后边会大幅减少
     */
    private void selectSpill() {
        double magicNum = 1.414; // TODO 这里太慢了,要不然直接挑第一个吧,似乎可以维护一个堆
        Operand m = spillList.stream().max((l, r) -> {
            double value1 = degree.getOrDefault(l, 0).doubleValue() / Math.pow(magicNum, loopDepths.getOrDefault(l, 0));
            double value2 = degree.getOrDefault(r, 0).doubleValue() / Math.pow(magicNum, loopDepths.getOrDefault(l, 0));
            return Double.compare(value1, value2);
        }).get();
//        Operand m = spillList.iterator().next();
        simplifyList.add(m);
        freezeMoves(m);
        spillList.remove(m);
    }
    private void assignColors(Function func) {
        HashMap<Operand, Operand> colored = new HashMap<>(); // colored 记录虚拟寄存器到物理寄存器的映射关系
        while (!selectStack.isEmpty()) {
            Operand n = selectStack.pop(); // 从栈上弹出一个节点
            HashSet<Integer> colors = new HashSet<>(PhysicsReg.canAllocateReg);
            for (Operand w : adjList.getOrDefault(n, new HashSet<>())) { // 遍历与这个弹出的节点相邻的节点
                Operand a = getAlias(w);
                if (a.hasAllocated() || a.isPrecolored()) { // 如果这个邻接点是物理寄存器,那么就要移除掉
                    colors.remove(((PhysicsReg) a).getNum());
                } else if (a instanceof VirtualReg) { // 如果邻接点是一个虚拟寄存器,而且已经被着色了
                    if (colored.containsKey(a)) {
                        Operand color = colored.get(a);
                        colors.remove(((PhysicsReg) color).getNum());
                    }
                }
            }
            if (colors.isEmpty()) {  // 如果没有备选颜色,那么就发生实际溢出
                spilledNodes.add(n);
            } else {
                Integer color = colors.iterator().next();
                colored.put(n, new PhysicsReg(color, true));
            }
        }
//        System.out.println("-----------------------");
        if (!spilledNodes.isEmpty()) return;
        // 当处理完 stack 后如果还没有问题,就可以处理合并节点
        for (Operand coalescedNode : coalescedNodes) { // 在一开始 stack 中只压入部分点（另一些点由栈中的点代表）
            Operand alias = getAlias(coalescedNode);
            if (alias.isPrecolored()) { // 如果合并的节点里有物理寄存器,而且还是一个预着色寄存器
                colored.put(coalescedNode, alias);
            } else {  // 如果全是虚拟寄存器
                colored.put(coalescedNode, colored.get(alias));
            }
        }
        for (Block block : func.getBlocks()) { // 完成替换
            List<Instruction> instructions = new CopyOnWriteArrayList<>(block.getInstructions());
            for (Instruction instr : instructions) {
                ArrayList<Reg> defs = new ArrayList<>(instr.getRegDef());
                ArrayList<Reg> uses = new ArrayList<>(instr.getRegUse());
                for (Reg def : defs) {
                    if (colored.containsKey(def)) {
                        instr.replaceReg(def, colored.get(def));
                    }
                }
                for (Reg use : uses) {
                    if (colored.containsKey(use)) {
                        instr.replaceReg(use, colored.get(use));
                    }
                }
            }
        }
    }
    /**
     * 确定加入溢出 load 和 store 的 offset
     * @param func 函数
     * @param instr load 或者 store 节点
     */
    private void fixOffset(Function func, Instruction instr) {
        int offset = func.getAllocaSize();
        Immediate mipsOffset = new Immediate(offset);
        if (instr instanceof Load mipsLoad) { // offset 的编码规则与之前不同
            mipsLoad.setOffset(mipsOffset);
        } else if (instr instanceof Store mipsStore) {
            mipsStore.setOffset(mipsOffset);
        }
    }
    /**
     * 处理溢出的临时变量插入到基本块中的功能
     * @param func 函数
     */
    private void checkPoint(Function func) {
        if (firstUseNode != null) {
            Load load = new Load(vReg, SP, null);
            firstUseBlock.addBefore(firstUseNode,load);
            fixOffset(func, load);
            firstUseNode = null;
        }
        if (lastDefNode != null) {
            Store store = new Store(vReg, SP, null);
            lastDefBlock.addAfter(lastDefNode,store);
            fixOffset(func, store);
            lastDefNode = null;
        }
        vReg = null;
    }
    private void rewriteProgram(Function func) {
        for (Operand n : spilledNodes) {
            LinkedList<Block> blocks = func.getBlocks();
            for (Block block : blocks) {
                vReg = null;firstUseNode = null;lastDefNode = null;
                int cntInstr = 0; // cntInstr 是 block 中已经处理的指令的个数
                List<Instruction> instructions = new CopyOnWriteArrayList<>(block.getInstructions());
                for (Instruction instr : instructions) {
                    HashSet<Reg> defs = new HashSet<>(instr.getRegDef());
                    HashSet<Reg> uses = new HashSet<>(instr.getRegUse());
                    for (Reg use : uses) {
                        if (use.equals(n)) {
                            if (vReg == null) {
                                vReg = new VirtualReg();
                                func.addUsedVirReg(vReg);
                            }
                            instr.replaceReg(n, vReg);
                            if (firstUseNode == null && lastDefNode == null) {
                                firstUseNode = instr;
                                firstUseBlock = block;
                            }
                        }
                    }
                    for (Reg def : defs) { // n 是最外层遍历的实际溢出的节点 (如果这个溢出的节点是目的寄存器)
                        if (def.equals(n)) { // 似乎只针对第一个等于 n 的目的寄存器
                            if (vReg == null) {
                                vReg = new VirtualReg();
                                func.addUsedVirReg(vReg);
                            }
                            instr.replaceReg(n, vReg);
                            lastDefNode = instr;
                            lastDefBlock = block;
//                            break;
                        }
                    }
                    if (cntInstr > 30) { // TODO 这里其实是一个权衡,改这里会不会让时间变快
                        checkPoint(func);
                    }
                    cntInstr++;
                }
                checkPoint(func);
            }
            func.addAllocaSize(4); // 为这个临时变量在栈上分配空间
        }
    }
    private void clearPhyRegState() {
        for (Function function : mipsModule.getFunctions()) {
            for (Block block : function.getBlocks()) {
                for (Instruction instr : block.getInstructions()) {
                    for (Reg reg : instr.getRegDef()) {
                        if (reg instanceof PhysicsReg) {
                            ((PhysicsReg) reg).setAllocated(false);
                        }
                    }
                    for (Reg reg : instr.getRegUse()) {
                        if (reg instanceof PhysicsReg) {
                            ((PhysicsReg) reg).setAllocated(false);
                        }
                    }
                }
            }
        }
    }
    public void process() {
        for (Function function : mipsModule.getNoBuiltInFunctions()) {
//            System.out.println("CurFunc : "+function.getName());
            boolean finished = false;
//            int i = 1;
            while (!finished) {
                init(function);build(function);makeList(function);
                while (!(simplifyList.isEmpty() && workListMoves.isEmpty() && freezeList.isEmpty() && spillList.isEmpty())){
                    if (!simplifyList.isEmpty()) simplify();
                    if (!workListMoves.isEmpty()) coalesce();
                    if (!freezeList.isEmpty()) freeze();
                    if (!spillList.isEmpty()) selectSpill();
                }
                assignColors(function);
//                System.out.println("process : "+i);i++;
                if (spilledNodes.isEmpty()) { // 看一下实际溢出的节点
                    finished = true;
                } else { // 存在实际溢出的点
                    rewriteProgram(function);
                }
            }
//            System.out.println("process finish!");
        }
        // 在 color 的时候,会把 isAllocated 设置成 true,这个函数的功能就是设置成 false
        clearPhyRegState(); // 为了避免物理寄存器在判定 equals 时的错误
        for (Function function : mipsModule.getNoBuiltInFunctions()) function.fixStack();
    }
}
