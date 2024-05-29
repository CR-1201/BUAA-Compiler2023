package ir.types;
/**
 @author Conroy
 函数与指令的返回值，包括 IntType, VoidType, PointerType
 */
public abstract class DataType extends valueType{
    @Override
    public abstract int getSize();
}
