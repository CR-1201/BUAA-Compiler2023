package backend.optimizer;

import tools.Pair;

import java.util.ArrayList;

public  class MulOptimizer {
    private int steps;
    private int multiplier; // source 是乘常数
    private final ArrayList<Pair<Boolean, Integer>> items; // bool 负责指示加减,integer 负责指示位移

    public MulOptimizer(int... shifts) {
        multiplier = 0;
        items = new ArrayList<>();
        for (int shift : shifts) {
            if (shift >= 0) {
                multiplier += 1 << shift;
                items.add(new Pair<>(true, shift & Integer.MAX_VALUE));
            }
            else {
                // i & Integer.MAX_VALUE 是 i + (1 << 31) 的意思
                multiplier -= 1 << (shift & Integer.MAX_VALUE);
                items.add(new Pair<>(false, shift & Integer.MAX_VALUE));
            }
        }
        steps = items.get(0).getFirst() || items.get(0).getSecond() == 0 ? 1 : 2; // 计算一下开销
        for (int i = 1; i < items.size(); i++) {
            steps += items.get(i).getSecond() == 0 ? 1 : 2;
        }
    }
    public int getSteps() {
        return steps;
    }
    public int getMultiplier() {
        return multiplier;
    }
    public ArrayList<Pair<Boolean, Integer>> getItems() {
        return items;
    }
    /**
     * 这个是基础步骤的步数的优化步数的比较
     * 此时的 base == 4
     * @return true 则采取优化
     */
    public boolean isBetter() {
        int base = 4;
        // 这里有个 base-- 我没有看懂
        if ((multiplier & 0xffff) == 0 ||
                (Short.MIN_VALUE <= multiplier && multiplier <= Short.MAX_VALUE - Short.MIN_VALUE)) {
            base--;
        }
        return steps < base;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("x * " + multiplier + " = ");
        for (int i = 0; i < items.size(); i++) {
            if (i != 0 || !items.get(i).getFirst()) {
                builder.append(items.get(i).getFirst() ? "+ " : "- ");
            }
            if (items.get(i).getSecond() == 0) {
                builder.append("x ");
            } else {
                builder.append("(x << ").append(items.get(i).getSecond()).append(") ");
            }
        }
        return builder.append("-> ").append(steps).toString();
    }
}
