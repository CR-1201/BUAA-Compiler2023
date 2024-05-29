package ir.constants;

import ir.types.ArrayType;
import ir.types.IntType;
import ir.types.valueType;
import ir.user;
import ir.value;

import java.util.ArrayList;
/**
 @author Conroy
 常量包括 ConstInt, ConstArray, ConstStr
 常量没有名字 也没有 parent
 */
public abstract class Constant extends user {

    public Constant(valueType valueType){
        super(null, valueType, null);
    }

    public Constant(valueType valueType, ArrayList<value> values){
        super(null, valueType, null, values);
    }
    /**
     * @param constantType Constant种类
     * @return 元素全0 的 ConstInt 或 ConstArray
     */
    public static Constant getZeroConstant(valueType constantType) {
        if (constantType instanceof IntType){
            return ConstInt.ZERO;
        }else{
            return ConstArray.getZeroConstantArray((ArrayType) constantType);
        }
    }
}
