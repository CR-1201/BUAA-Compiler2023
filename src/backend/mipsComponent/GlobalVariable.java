package backend.mipsComponent;

import java.util.ArrayList;

public class GlobalVariable {
    private final String name;
    private final boolean hasInit; // 是否初始化
    private final boolean isStr; // 是否是字符串
    private final int size;
    private final ArrayList<Integer> elements;
    private final String content;
    public GlobalVariable(String name, ArrayList<Integer> elements) {
        this.name = name.substring(1);
        this.hasInit = true;
        this.isStr = false;
        this.size = 4 * elements.size();
        this.elements = elements;
        this.content = null;
    }
    public GlobalVariable(String name, int size) {
        this.name = name.substring(1);
        this.hasInit = false;
        this.isStr = false;
        this.size = size;
        this.elements = null;
        this.content = null;
    }
    public GlobalVariable(String name, String content) {
        this.name = name.substring(1);
        this.hasInit = true;
        this.isStr = true;
        this.size = content.length() + 1;
        this.elements = null;
        this.content = content;
    }
    /**
     * 只要是用 .word 初始化的,那么就是对齐的,否则是不对齐的
     * @return 对其为 true
     */
    public boolean isAlign() {
        return hasInit && !isStr;
    }
    public boolean hasInit() {
        return hasInit;
    }
    /**
     * 根据是否是 init 选择打印方式
     * @return 全局变量字符串
     */
    @Override
    public String toString() {
        StringBuilder global = new StringBuilder("");
        global.append(name).append(": ");
        if (hasInit) { // 初始化了就用 .word 或者 .ascii
            if (isStr) {
                global.append(".asciiz \"").append(content).append("\"");
            } else {
                global.append(".word ");
                for (Integer element : elements) {
                    global.append(element).append(",");
                }
                global.delete(global.length()-1,global.length());
            }
        } else { // 未初始化就用 .space
            global.append(".space ").append(size);
        }
        return global.toString();
    }
}
