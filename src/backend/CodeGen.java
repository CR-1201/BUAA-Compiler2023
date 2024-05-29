package backend;

import backend.mipsComponent.Block;
import backend.mipsComponent.Function;
import backend.mipsComponent.Module;
import backend.mipsInstruction.*;
import backend.optimizer.MulOptimizer;
import backend.reg.*;
import ir.Argument;
import ir.BasicBlock;
import ir.constants.*;
import ir.instructions.Binary_Instructions.*;
import ir.instructions.Other_Instructions.PHI;
import ir.value;
import tools.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import static backend.mipsInstruction.CondType.genCond;
import static backend.reg.PhysicsReg.*;
/**
 * @author Conroy
 * 主打一个不面向对象, 想 public 就 public, 拿我怎样
 */
public class CodeGen {
    private static final CodeGen codeGen = new CodeGen();
    public CodeGen() {
        this.irModule = ir.module.getModule();
        this.mipsModule = new Module();
    }
    public static CodeGen getCodeGen(){
        return codeGen;
    }
    private final ir.module irModule;
    public final Module mipsModule;
    /**
     * 用于提供 ir 函数到 mips 函数的映射
     */
    public static final HashMap<ir.Function, Function> functionHashMap= new HashMap<>();
    /**
     * 用于提供 ir 块到 mips 块的映射
     */
    public static final HashMap<ir.BasicBlock, Block> blockBlockHashMap = new HashMap<>();
    /**
     *  map 中登记 instr 的目的寄存器, arg 参数, 不会登记 imm, float, label
     */
    public static final HashMap<value, Operand> operandMap = new HashMap<>();
    /**
     * 这个 map 会根据 被除数和除数对 来查询之前的运算结果
     * 之所以要加上 block 信息, 是因为 block 限制了历史, 不是所有的 block 都会被运行到的
     */
    public static final HashMap<Pair<Block, Pair<Operand, Operand>>, Operand> divMap = new HashMap<>();
    /**
     * 用于记录已经加载过的全局变量, 必须局限于基本块内
     */
    public static final HashMap<Pair<Block, ir.GlobalVariable>, Operand> globalVariableMap = new HashMap<>();
    /**
     * key 是基本块的 <前驱,后继> 关系,查询出来的 Arraylist 是需要插入到两个块之间的指令（一般是插入到前驱块尾部）,这样可以实现 phi 的选择功能
     */
    public static final HashMap<Pair<Block, Block>, ArrayList<Instruction>> phiLists = new HashMap<>();
    /**
     * 可以根据乘常数查询对应的优化序列
     */
    private static final HashMap<Integer, MulOptimizer> mulOptimizers = new HashMap<>();
    static {
        buildMulOptimizers(); // 初始化 mulOptimizers
    }
    /**
     * 解析的主函数
     * @return 组织好的 mipsModule
     */
    public Module buildModule() {
        irModule.buildGlobalVariables();
        irModule.buildFunctions();
        return mipsModule;
    }
    public Module getMipsModule(){
        return mipsModule;
    }
    /**
     * 将 ir 中发挥 Operand 作用的 value 转成一个 mips Operand
     * 注: 对于一个操作数, 可能是一个字面值, 也可能是指令的执行结果, 也可能是函数的参数, 还可能是全局变量
     * 进一步理解, 操作数是 mips 数据结构的最底层, 它应该是所有逻辑的起点, 几乎所有的 value 都可以成为操作数
     * @return 一个操作数
     */
    public Operand buildOperand(value irValue, boolean allow_Imm, ir.Function irFunction, BasicBlock irBlock) {
        if (operandMap.containsKey(irValue)) { // 如果已经被解析了,那么就不需要再解析了
            Operand mipsOperand = operandMap.get(irValue); // 直接在堆里找即可
            if ( mipsOperand instanceof Immediate && !allow_Imm ) {  // 但是如果是立即数,而逻辑中又不允许立即数,那么就需要重新 move
                if (((Immediate) mipsOperand).getImmediate() == 0) { // 如果立即数是 0, 直接返回 $zero
                    return ZERO;
                } else {
                    Operand tmp = getTmpReg(irFunction);
                    Move mipsMove = new Move(tmp, mipsOperand);
                    blockBlockHashMap.get(irBlock).addInstrTail(mipsMove);
                    return tmp;
                }
            } else {
                return mipsOperand;
            }
        } else if (irValue instanceof Argument && irFunction.getArguments().contains(irValue)) { //保险加了个 &&
            return buildArgOperand((Argument) irValue, irFunction);
        } else if (irValue instanceof ir.GlobalVariable) {
            return buildGlobalOperand((ir.GlobalVariable) irValue, irFunction, irBlock);
        } else if (irValue instanceof ConstInt) { // 如果是整型常数
            return buildConstIntOperand(((ConstInt) irValue).getValue(), allow_Imm, irFunction, irBlock);
        } else { // 如果是指令,那么需要生成一个目的寄存器
            return buildDstOperand(irValue, irFunction);
        }
    }
    /**
     * 生成一个临时中转的寄存器,来翻译诸如 1 + 2 * 3 这种无法一个 mips 指令完成的操作
     * @return 一个虚拟寄存器
     */
    public Operand getTmpReg(ir.Function irFunction) {
        Function mipsFunction = functionHashMap.get(irFunction);
        VirtualReg tmpReg = new VirtualReg();
        mipsFunction.addUsedVirReg(tmpReg);
        return tmpReg;
    }

    /**
     * Counts the number of Tailing Zeros
     * 计算最低位 1 的位数,如果输入 0,返回 0
     * 类似于计算 log2(n)
     * @param n 待输入的数
     * @return 最低位 1 的位数
     */
    public static int log2(int n) {
        int res = 0;n = n >>> 1;
        while (n != 0) {
            n = n >>> 1;res++;
        }
        return res;
    }
    /**
     * 在高级语言中,除法是向 0 取整的,也就是说 3 / 4 = 0, -3 / 4 = 0
     * 但是如果用移位操作来处理的话,除法是向下取整的,即 3 / 4 = 0, -3 / 4 = -1
     * 所以为了适应高级语言,我们需要产生新的被除数,有 newDividend = oldDividend + divisor - 1
     * @param oldDividend 旧的被除数
     * @param l        log2(div)
     * @param irBlock     当前块
     * @param irFunction  当前函数
     * @return 新的被除数
     */
    public Operand buildCeilDividend(Operand oldDividend, int l, BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        Operand tmp1 = getTmpReg(irFunction);
        Shift shift1 = Shift.Sra(tmp1, oldDividend, 31); // 获取最高位
        // 然后将那一堆 1 或者 0 逻辑右移 32 - l 位
        // 这样就会在 [l-1 : 0] 位获得一堆 1 或者 0,其实就是 2^l - 1 = abs - 1
        // 最后将这 abs - 1 加到被除数上, 完成了针对负数的向上取整操作
        Operand tmp2 = getTmpReg(irFunction);
        Shift shift2 = Shift.Srl(tmp1, tmp1, 32 - l); // 注意,只有在被除数是负数的时候有效;
        Binary addu = Binary.Addu(tmp2, oldDividend, tmp1); // 如果被除数是正数, tem1 == 0, 就多了两条指令, 但是相比繁琐的除法, 是值得的;
        mipsBlock.addInstrTail(shift1);
        mipsBlock.addInstrTail(shift2);
        mipsBlock.addInstrTail(addu);
        return tmp2;
    }

    /**
     * 对于 buildIcmp,当两个 OP 均为常数的时候,直接在 OperandMap 中添加
     * 否则就需要 set 指令了
     * 在 MIPS 中,set 类指令只有 slt, slti 两种,其他的指令都是拓展指令
     * 而且在 MARS 提供的伪指令模板中,对于这两种指令的优化并不好
     * @param instr      比较指令
     * @param irBlock    当前块
     * @param irFunction 当前函数
     */
    public void buildIcmp(ICMP instr, BasicBlock irBlock, ir.Function irFunction) {
        Operand dst = buildOperand(instr, false, irFunction, irBlock);
        CondType cond = genCond(instr.getCondition());
        if (instr.getOp1() instanceof ConstInt && instr.getOp2() instanceof ConstInt) { // 如果均是常数,那么直接比较即可
            int op1 = ((ConstInt) instr.getOp1()).getValue();
            int op2 = ((ConstInt) instr.getOp2()).getValue();
            dst = new Immediate(cond.compare(op1, op2) ? 1 : 0);
            operandMap.put(instr, dst);
        } else {
            switch (cond) { // 对不同情况分类讨论,对于前四种情况,是有具体方法对应的,后两种,可以通过交换操作数顺序套用原有模板
                case EQ -> {
                    eqTemplate(dst, instr.getOp1(), instr.getOp2(), irBlock, irFunction);
                }
                case NE -> {
                    neTemplate(dst, instr.getOp1(), instr.getOp2(), irBlock, irFunction);
                }
                case LE -> {
                    leTemplate(dst, instr.getOp1(), instr.getOp2(), irBlock, irFunction);
                }
                case LT -> {
                    ltTemplate(dst, instr.getOp1(), instr.getOp2(), irBlock, irFunction);
                }
                case GE -> {
                    leTemplate(dst, instr.getOp2(), instr.getOp1(), irBlock, irFunction);
                }
                case GT -> {
                    ltTemplate(dst, instr.getOp2(), instr.getOp1(), irBlock, irFunction);
                }
            }
        }
    }
    /**
     * 如果允许返回立即数,那么就返回立即数,因为对于 mips 来说,有足够多的伪指令可以使得直接处理 32 位数
     * 如果不允许返回立即数,那么就返回一个 li 的结果
     * 此外,考虑到 at 寄存器不参与分配,所以可以将目的寄存器设置成 at
     * 即使出现 li at, -100000 这种指令,依然在 MARS 中是可以正常工作的
     * 那么需不需要考虑对于 add $t0, $at, $at 这样的指令呢,就是都需要 at 寄存器去加载
     * 然后就会导致靠前加载的值会被靠后加载的值覆盖
     * 是不会的,因为这种指令就可以直接算出来了
     * 只需要注意分配寄存器的时候,不分配 at 寄存器即可,这样就不会出现 at 被覆盖的情况
     * @param imm        立即数
     * @param irFunction 所在的函数
     * @param irBlock    所在的 block
     * @param canImm     表示允不允许是一个立即数
     * @return 操作数
     */
    public Operand buildConstIntOperand(int imm, boolean canImm, ir.Function irFunction, BasicBlock irBlock) {
        Immediate mipsImm = new Immediate(imm);
        if (canEncodeImm(imm, true) && canImm) { // 如果可以直接编码而且允许返回立即数, 那么就直接返回就可以了
            return mipsImm;
        } else { // 不可以立即数
            if (imm == 0) { // 如果需要的立即数是 0,那么就直接返回 $zero 寄存器
                return new PhysicsReg(0);
            } else { // 否则就用 li $at, imm 来加载
                Block mipsBlock = blockBlockHashMap.get(irBlock);
                Function mipsFunction = functionHashMap.get(irFunction);
                VirtualReg dst = new VirtualReg();
                mipsFunction.addUsedVirReg(dst);
                Move mipsMove = new Move(dst, mipsImm);
                mipsBlock.addInstrTail(mipsMove);
                return dst;
            }
        }
    }
    /**
     * 会将指令插入到函数的头部
     * @param irArgument 参数
     * @param irFunction 函数
     * @return 拥有函数参数的寄存器
     */
    private Operand buildArgOperand(Argument irArgument, ir.Function irFunction) {
        Function mipsFunction = functionHashMap.get(irFunction);
        int num = irArgument.getNum(); // 是第几个参数
        Block firstBlock = blockBlockHashMap.get((BasicBlock) irFunction.getBasicBlocks().getFirst());
        // 如果是浮点数,那么就需要用浮点数传参,因为浮点传参是 s0 - s15,不打算考虑到栈的情况了
        VirtualReg dstVirReg = new VirtualReg();
        operandMap.put(irArgument, dstVirReg);
        mipsFunction.addUsedVirReg(dstVirReg);
        if (num < 4) { // 创建一个移位指令
            Move mipsMove = new Move(dstVirReg, new PhysicsReg("$a" + num));
            firstBlock.addInstrHead(mipsMove);
        } else { // 这时需要从栈上加载
            int stackPos = num - 4; // 创建一个移位指令
            Immediate mipsOffset = new Immediate(stackPos * 4);
            mipsFunction.addArgOffset(mipsOffset); // 这里有改成了 offset,因为 mips 支持这样的伪指令
            Load load = new Load(dstVirReg, SP, mipsOffset); // 创建一个加载指令
            firstBlock.addInstrHead(load); // 这个指令需要插入到头部
        }
        return dstVirReg;
    }
    /**
     * 全局变量使用前需要加载到一个虚拟寄存器中（直接使用的方法似乎在分段 .data 的时候不成立）
     * @param irGlobal 全局变量
     * @return 操作数
     */
    private Operand buildGlobalOperand(ir.GlobalVariable irGlobal, ir.Function irFunction, BasicBlock irBlock) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        Pair<Block, ir.GlobalVariable> globalLoopUp = new Pair<>(mipsBlock, irGlobal);
        if (globalVariableMap.containsKey(globalLoopUp)) {
            return globalVariableMap.get(globalLoopUp);
        } else {
            Operand dst = getTmpReg(irFunction); // 这里没有使用 at,是因为使用 at 没有意义,要记录下来,at 的值不容易记
            Move mipsMove = new Move(dst, new Label(irGlobal.getName().substring(1)));// 这个 move 最后会变成 la
            mipsBlock.addInstrTail(mipsMove);
            return dst;
        }
    }
    /**
     * 用于生成目的寄存器, 可以看做是解析指令的结果
     * 最明显的是,这个指令的类型决定了目的寄存器的类型
     * @param irValue    应该是指令
     * @param irFunction 所在的函数
     * @return 目的寄存器
     */
    public Operand buildDstOperand(value irValue, ir.Function irFunction) {
        Function mipsFunction = functionHashMap.get(irFunction);
        VirtualReg dstReg = new VirtualReg();
        mipsFunction.addUsedVirReg(dstReg);
        operandMap.put(irValue, dstReg);
        return dstReg;
    }
    /**
     * 对于一个 16 位数,这个就是标杆
     * @param imm 立即数
     * @return true 可以编码
     */
    public static boolean canEncodeImm(int imm, boolean isSignExtend) {
        if (isSignExtend) {
            return Short.MIN_VALUE <= imm && imm <= Short.MAX_VALUE;
        } else {
            return 0 <= imm && imm <= (Short.MAX_VALUE - Short.MIN_VALUE);
        }
    }

    public static ArrayList<Pair<Boolean, Integer>> getMulOptItems(int multiplier) {
        if (mulOptimizers.containsKey(multiplier)) {
            return mulOptimizers.get(multiplier).getItems();
        } else {
            return new ArrayList<>();
        }
    }
    /**
     * 输入一个寄存器和一个 imm,最终会使得 at 寄存器中存入一个值
     * 如果 src 和 imm 的值相等,那么 at 寄存器为 0,否则不为 0
     * 要求 src 必须是寄存器,不能是立即数
     * @param src 源寄存器
     * @param imm 比较立即数
     * @param irBlock 基本块
     * @param irFunction 函数
     */
    private void basicEqTemplate(Operand src, int imm, BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
        if (canEncodeImm(-imm, true)) {
            Binary addu = Binary.Addu(AT, src, new Immediate(-imm));
            mipsBlock.addInstrTail(addu);
        } else if (canEncodeImm(imm, false)) {
            Binary xor = Binary.Xor(AT, src, new Immediate(imm));
            mipsBlock.addInstrTail(xor);
        } else { // 这里就相当于用 li at, imm
            Operand mipsAt = buildConstIntOperand(imm, true, irFunction, irBlock);
            Binary xor = Binary.Xor(AT, src, mipsAt); // xor at, src, at
            mipsBlock.addInstrTail(xor);
        }
    }
    /**
     * 基本思路是这样的,对于 a 和 b 的比较
     * 让 a 和 b xor 或者相减,达到 !(a == b) 的效果
     * 然后 !(a == b) < 1 （也就是 !(a == b) == 0）就可以完成比较
     * eq 具有某种意义上的交换性,所以可以更加方便的调整
     * @param dst 目标寄存器
     * @param op1 第一个操作数
     * @param op2 第二个操作数
     * @param irBlock 基本块
     * @param irFunction 函数
     */
    private void eqTemplate(Operand dst, value op1, value op2, BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
//        mipsBlock.addInstrTail(new Comment("eq " + op1.getName() + " " + op2.getName()));
        if (op1 instanceof ConstInt) {
            Operand src = buildOperand(op2, false, irFunction, irBlock);
            basicEqTemplate(src, ((ConstInt) op1).getValue(), irBlock, irFunction);
            Binary sltu = Binary.Sltu(dst, AT, new Immediate(1));
            mipsBlock.addInstrTail(sltu);
        } else if (op2 instanceof ConstInt) {
            Operand src = buildOperand(op1, false, irFunction, irBlock);
            basicEqTemplate(src, ((ConstInt) op2).getValue(), irBlock, irFunction);
            Binary sltu = Binary.Sltu(dst, AT, new Immediate(1));
            mipsBlock.addInstrTail(sltu);
        } else {
            PhysicsReg tmpReg = AT;
            Operand mipsOp1 = buildOperand(op1, false, irFunction, irBlock); // 这两个东西不会在 at 里,只有大的 imm 会在里面,而这里显然没有 imm 了
            Operand mipsOp2 = buildOperand(op2, false, irFunction, irBlock);
            Binary xor = Binary.Xor(tmpReg, mipsOp1, mipsOp2);
            mipsBlock.addInstrTail(xor);
            Binary sltu = Binary.Sltu(dst, tmpReg, new Immediate(1));
            mipsBlock.addInstrTail(sltu);
        }
    }
    /**
     * 思路与 eqTemplate 类似,首先拿到 !(a == b)
     * 然后利用 0 < !(a == b) 完成比较
     */
    private void neTemplate(Operand dst, value op1, value op2, BasicBlock irBlock, ir.Function irFunction) {
        Block mipsBlock = blockBlockHashMap.get(irBlock);
//        mipsBlock.addInstrTail(new Comment("ne " + op1.getName() + "\t" + op2.getName()));
        if (op1 instanceof ConstInt) {
            Operand src = buildOperand(op2, false, irFunction, irBlock);
            basicEqTemplate(src, ((ConstInt) op1).getValue(), irBlock, irFunction);
            Binary mipsSltu = Binary.Sltu(dst, ZERO, AT);
            mipsBlock.addInstrTail(mipsSltu);
        } else if (op2 instanceof ConstInt) {
            Operand src = buildOperand(op1, false, irFunction, irBlock);
            basicEqTemplate(src, ((ConstInt) op2).getValue(), irBlock, irFunction);
            Binary sltu = Binary.Sltu(dst, ZERO, AT);
            mipsBlock.addInstrTail(sltu);
        } else {
            PhysicsReg tmpReg = AT;
            Operand ojbOp1 = buildOperand(op1, false, irFunction, irBlock);
            Operand mipsOp2 = buildOperand(op2, false, irFunction, irBlock);
            Binary xor = Binary.Xor(tmpReg, ojbOp1, mipsOp2);
            mipsBlock.addInstrTail(xor);
            Binary sltu = Binary.Sltu(dst, ZERO, tmpReg);
            mipsBlock.addInstrTail(sltu);
        }
    }
    /**
     * 主要用到的是 a <= b 就是 !(a > b) 就是 !(b < a) 的特性
     */
    private void leTemplate(Operand dst, value op1, value op2, BasicBlock irBlock, ir.Function irFunction) { // 简单直白
        Block mipsBlock = blockBlockHashMap.get(irBlock);
//        mipsBlock.addInstrTail(new Comment("le " + op1.getName() + " " + op2.getName()));
        PhysicsReg tmp = AT;
        Operand src1 = buildOperand(op1, true, irFunction, irBlock); // < 不具有交换性,就和减法一样,只能放弃抵抗
        Operand src2 = buildOperand(op2, false, irFunction, irBlock);
        Binary slt = Binary.Slt(tmp, src2, src1);// 通过交换操作数,达到 > 的目的
        mipsBlock.addInstrTail(slt);
        Binary xor = Binary.Xor(dst, tmp, new Immediate(1));// 对 > 取反,就变成了 <=
        mipsBlock.addInstrTail(xor);
    }
    private void ltTemplate(Operand dst, value op1, value op2, BasicBlock irBlock, ir.Function irFunction) { // 简单直白
        Block mipsBlock = blockBlockHashMap.get(irBlock);
//        mipsBlock.addInstrTail(new Comment("lt " + op1.getName() + " " + op2.getName()));
        Operand src1 = buildOperand(op1, false, irFunction, irBlock);
        Operand src2 = buildOperand(op2, true, irFunction, irBlock);
        Binary slt = Binary.Slt(dst, src1, src2);
        mipsBlock.addInstrTail(slt);
    }

    public static void buildMulOptimizers(){    // 这段代码的目的是生成 mulOptimizers
        ArrayList<MulOptimizer> tmpLists = new ArrayList<>();
        int TAG = 0x80000000;
        for (int i = 0; i < 32; i++) { // 因为基准是 4,所以最多可以采用 3 个正向 shift,所以有 i, j, k 三个
            tmpLists.add(new MulOptimizer(i));
            tmpLists.add(new MulOptimizer(i | TAG));
            for (int j = 0; j < 32; j++) {
                tmpLists.add(new MulOptimizer(i, j));
                tmpLists.add(new MulOptimizer(i, j | TAG));
                tmpLists.add(new MulOptimizer(i | TAG, j));
                tmpLists.add(new MulOptimizer(i | TAG, j | TAG));
                for (int k = 0; k < 32; k++) {
                    tmpLists.add(new MulOptimizer(i, j, k));
                    tmpLists.add(new MulOptimizer(i, j, k | TAG));
                    tmpLists.add(new MulOptimizer(i, j | TAG, k));
                    tmpLists.add(new MulOptimizer(i, j | TAG, k | TAG));
                    tmpLists.add(new MulOptimizer(i | TAG, j, k));
                    tmpLists.add(new MulOptimizer(i | TAG, j, k | TAG));
                    tmpLists.add(new MulOptimizer(i | TAG, j | TAG, k));
                    tmpLists.add(new MulOptimizer(i | TAG, j | TAG, k | TAG));
                }
            }
        }
        for (MulOptimizer tmp : tmpLists) { // 通过这个筛选,获得比基准情况和其他优化情况更优的优化
            if (tmp.isBetter()) {
                if (!mulOptimizers.containsKey(tmp.getMultiplier()) ||
                        tmp.getSteps() < mulOptimizers.get(tmp.getMultiplier()).getSteps()) {
                    mulOptimizers.put(tmp.getMultiplier(), tmp);
                }
            }
        }
    }
    /**
     * 这个函数会根据当前块和其某个前驱块,生成要插入这个前驱块的 mov 指令（通过 phi 翻译获得,我们称为 moves）
     * @param phis       当前块的 phi 指令集合
     * @param irFunction 当前 ir 函数
     * @param irBlock    当前区块
     * @return 一堆待插入的 move 指令
     */
    public ArrayList<Instruction> buildPHI(ArrayList<PHI> phis, BasicBlock irPreBlock, ir.Function irFunction, BasicBlock irBlock) {
        Function mipsFunction = functionHashMap.get(irFunction);
        HashMap<Operand, Operand> graph = new HashMap<>(); // 通过构建一个图来检验是否成环
        ArrayList<Instruction> moves = new ArrayList<>();
        for (PHI phi : phis) { // 构建一个图
            Operand phiTarget = buildOperand(phi, false, irFunction, irBlock);
            value inputValue = phi.getInputVal(irPreBlock); // phiSrc phi 目的寄存器可能的一个值
            Operand phiSrc;
            if (inputValue instanceof ConstInt constInt) {
                phiSrc = new Immediate(constInt.getValue());
            } else {
                phiSrc = buildOperand(inputValue, true, irFunction, irBlock);
            }
            graph.put(phiTarget, phiSrc);
        }
        while (!graph.isEmpty()) {
            Stack<Operand> path = new Stack<>();
            Operand cur;
            for (cur = graph.entrySet().iterator().next().getKey(); graph.containsKey(cur); cur = graph.get(cur)) { // 对这个图进行 DFS 遍历来获得成环信息, DFS 发生了不止一次, 而是每次检测到一个环就会处理一次
                if (path.contains(cur)) {
                    break;
                } else {
                    path.push(cur);
                }
            }
            if (!graph.containsKey(cur)) { //如果以该点出发没有环路
                handleNoCyclePath(path, cur, moves, graph);
            } else {
                handleCyclePath(mipsFunction, path, cur, moves, graph);
                handleNoCyclePath(path, cur, moves, graph);
            }
        }
        return moves;
    }
    private void handleNoCyclePath(Stack<Operand> path, Operand begin, ArrayList<Instruction> moves, HashMap<Operand, Operand> graph) {
        Operand phiSrc = begin;
        while (!path.isEmpty()) {
            Operand phiTarget = path.pop();
            Instruction mipsMove = new Move(phiTarget, phiSrc);
            moves.add(0, mipsMove);
            phiSrc = phiTarget;
            graph.remove(phiTarget);
        }
    }
    private void handleCyclePath(Function mipsFunction, Stack<Operand> path, Operand begin, ArrayList<Instruction> moves, HashMap<Operand, Operand> graph) {
        VirtualReg tmp = new VirtualReg();
        mipsFunction.addUsedVirReg(tmp);
        Move mipsMove = new Move(null, null);
        mipsMove.setDst(tmp);
        while (path.contains(begin)) {
            Operand r = path.pop();
            mipsMove.setSrc(r);
            moves.add(mipsMove);
            mipsMove = new Move(null, null);
            mipsMove.setDst(r);
            graph.remove(r);
        }
        mipsMove.setSrc(tmp);
    }
}
