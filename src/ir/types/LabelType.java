package ir.types;
/**
 @author Conroy
 标签,用于 BasicBlock
 */

public class LabelType extends valueType{
    @Override
    public String toString(){
        return "label";
    }
    @Override
    public int getSize(){
        throw new AssertionError("get label's size!");
    }
}
