package tools;

import java.util.Objects;

/**
 * 一个简单的 pair 类
 * @param <T_1> 第一个参数的类型
 * @param <T_2> 第二个参数的类型
 */
public class Pair<T_1,T_2> {
    private T_1 first;
    private T_2 second;

    public Pair(T_1 first, T_2 second) {
        this.first = first;
        this.second = second;
    }

    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    public T_1 getFirst() {
        return first;
    }

    public void setFirst(T_1 first) {
        this.first = first;
    }

    public T_2 getSecond() {
        return second;
    }

    public void setSecond(T_2 second) {
        this.second = second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }
}
