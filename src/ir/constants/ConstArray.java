package ir.constants;

import ir.types.ArrayType;
import ir.types.valueType;
import ir.value;

import java.util.ArrayList;
/**
 @author Conroy
 一个数组,包含该 ConstantArray 中的所有元素
 */
public class ConstArray extends Constant {
    private final ArrayList<Constant> elements = new ArrayList<>();
    /**
     * elements 是常量数组,里面有一堆常量,可能是 ConstInt,也可能是 ConstArray
     * 构造它的类型的时候,需要元素的信息,这里选择了第一个元素
     * @param elements 常量数组
     */
    public ConstArray(ArrayList<Constant> elements){
        super(new ArrayType(elements.get(0).getValueType(), elements.size()),new ArrayList<>(elements));
        this.elements.addAll(elements);
    }

    public Constant getElementByIndex(int index){
        return elements.get(index);
    }

    public static ConstArray getZeroConstantArray(ArrayType arrayType){
        ArrayList<Constant> elements = new ArrayList<>();
        int n = arrayType.getElementNum();
        valueType type = arrayType.getElementType();
        for (int i = 0; i < n; i++){
            elements.add(Constant.getZeroConstant(type));
        }
        return new ConstArray(elements);
    }
    /**
     * @return 数组展开后的一维数组,每个元素都是一个 ConstInt
     */
    public ArrayList<ConstInt> getDataElements(){
        ArrayList<ConstInt> result = new ArrayList<>();
        // 当前数组是一维数组
        if (elements.get(0) instanceof ConstInt){
            for (Constant element : elements){
                result.add((ConstInt) element);
            }
        }else{ // 二维数组
            for (Constant element : elements){
                result.addAll(((ConstArray) element).getDataElements());
            }
        }
        return result;
    }

    @Override
    public String toString(){
        StringBuilder s = new StringBuilder("[");
        // ConstArray 里面没有保存信息,只能依赖于标签信息
        int n = ((ArrayType) getValueType()).getElementNum();
        for (int i = 0; i < n; i++){
            s.append(getValue(i).getValueType()).append(" ").append(getValue(i));
            if( i+1 < n )s.append(", ");
        }
        s.append("]");
        return s.toString();
    }
}
