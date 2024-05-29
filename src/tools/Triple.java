package tools;

public class Triple {
    Object first;
    Object second;
    Object third;

    public Triple(Object first, Object second, Object third) {
        this.first = first;
        this.second = second;
        this.third = third;
    }

    public Object getThird() {
        return third;
    }

    public Object getFirst() {
        return first;
    }

    public Object getSecond() {
        return second;
    }
}
