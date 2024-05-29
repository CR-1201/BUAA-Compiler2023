package ir.instructions.Terminator_Instructions;

import ir.BasicBlock;
import ir.instructions.instruction;
import ir.types.DataType;
import ir.value;

import java.util.ArrayList;
/**
 @author Conroy
 终结指令,包括 Ret 和 Br
 */
public class TerInstruction extends instruction {
    public TerInstruction(DataType dataType, BasicBlock parent, ArrayList<value> ops){
        super("", dataType, parent, ops);
    }
}