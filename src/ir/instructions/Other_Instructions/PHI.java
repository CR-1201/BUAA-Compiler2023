package ir.instructions.Other_Instructions;

import ir.BasicBlock;
import ir.instructions.instruction;
import ir.types.DataType;
import ir.value;

import java.util.ArrayList;
import java.util.Arrays;

public class PHI extends instruction {
    private int precursorNum;

    public PHI(int nameNum, DataType dataType, BasicBlock parent, int precursorNum) {
        super("%p" + nameNum, dataType, parent, new ArrayList<>(Arrays.asList(new value[precursorNum * 2])));
        this.precursorNum = precursorNum;
    }

    public int getPrecursorNum(){
        return precursorNum;
    }
    public value getInputVal(BasicBlock block) {
        for (int i = 0; i < precursorNum; i++) {
            if (getValue(i + precursorNum) == block) {
                return getValue(i);
            }
        }
        throw new AssertionError("block not found for phi!");
    }
    /**
     * 移除冗余的 phi,比如说所有的 input 都相等的情况
     * @param reducePhi 是否进行
     */
    public void optimizePHI(boolean reducePhi){
        if (getUsers().isEmpty()){
            removeAllOperators();
            eraseFromParent();
            return;
        }
        if (getPrecursorNum() == 0){
            throw new AssertionError(this + "'s precursorNum = 0!");
        }
        value commonValue = getValue(0);
        for (int i = 1; i < getPrecursorNum(); i++){
            if (commonValue != getValue(i)){
                return;
            }
        }
        if (!reducePhi && commonValue instanceof instruction){
            return;
        }
        selfReplace(commonValue);
        removeAllOperators();
        eraseFromParent();
    }
    public void addIncoming(value value, BasicBlock block) {
        int i = 0;
        while (i < getPrecursorNum() && getValue(i) != null) {
            i++;
        }
        if (i < getPrecursorNum()) {
            updateValue(i, value);
            updateValue(i + getPrecursorNum(), block);
        } else {
            getOperators().add(getPrecursorNum(), value);
            precursorNum++;
            getOperators().add(block);
        }
        value.addUser(this);
        block.addUser(this);
    }

    @Override
    public String toString(){
        StringBuilder s = new StringBuilder(getName() + " = phi ").append(getValueType());
        for (int i = 0; i < precursorNum; i++){
            if (getValue(i) == null) break;
            s.append(" [ ").append(getValue(i).getName()).append(", ")
                    .append(getValue(i + precursorNum).getName()).append(" ]");
            if( i+1 < precursorNum )s.append(", ");
        }
        return s.toString();
    }
}
