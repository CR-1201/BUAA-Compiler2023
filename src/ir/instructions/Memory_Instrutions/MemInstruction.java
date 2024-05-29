package ir.instructions.Memory_Instrutions;

import ir.BasicBlock;
import ir.instructions.instruction;
import ir.types.DataType;
import ir.value;

import java.util.ArrayList;

/**
 @author Conroy
 内存访问与寻址
 包括 load, store, alloca, GEP
 */
public class MemInstruction extends instruction{
    MemInstruction(String name, DataType dataType, BasicBlock parent, ArrayList<value> ops){
        super(name, dataType, parent, ops);
    }
}