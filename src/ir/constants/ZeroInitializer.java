package ir.constants;

import ir.types.ArrayType;

public class ZeroInitializer extends Constant{
    private final int length;
    public ZeroInitializer(ArrayType arrayType){
        super(arrayType);
        length = arrayType.getElementNum();
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString(){
        return "zeroinitializer";
    }
}
