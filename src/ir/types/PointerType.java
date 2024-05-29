package ir.types;
/**
 @author Conroy
 指针类型,可以指向:
    IntegerType
    ArrayType
    PointerType,最多有二重指针
 */
public class PointerType extends DataType{
    /**
     * 该指针指向的标签
     */
    private final valueType pointeeType;
    /**
     * @param pointeeType 指向的元素类型, 可以为 ArrayType | IntType | FPType | PointerType（最多二重指针）
     *                    int a[10]     -- %1 = alloca [10 * i32] 中的 %1 需要视为指向int [10]的指针,而不是指向int的指针
     *                    int b[10][20] -- %2 = alloca [10 * [20 * i32]] 中的 %2 需要视为int [10][20]的指针,而不是指向 int [20]的指针
     *                    这一点与 C 语言不一样
     */
    public PointerType(valueType pointeeType){
        this.pointeeType = pointeeType;
    }
    public valueType getPointeeType(){
        return pointeeType;
    }
    @Override
    public String toString(){
        return pointeeType + "*";
    }
    @Override
    public int getSize(){
        return 4;
    }
}
