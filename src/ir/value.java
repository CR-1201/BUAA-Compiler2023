package ir;

import backend.mipsInstruction.Move;
import backend.reg.Immediate;
import backend.reg.Operand;
import ir.constants.ConstInt;
import ir.types.valueType;

import java.util.ArrayList;
import java.util.Objects;

import static backend.CodeGen.*;
import static backend.reg.PhysicsReg.ZERO;
import static config.Config.irError;

/**
 @author Conroy
 最 basic 的类,llvm ir 的一切元素均是 value
 value id 唯一
 */

public abstract class value {
    private final int id; // 唯一标识
    protected String name; // value 的名字,用于打印 llvm ir
    private final valueType valueType;
    private value parent; // value 的拥有者,注意不是使用者 user
    /**
     * 记录使用过当前 value 的使用者,一个 value 可以有多个 user
     */
    private final ArrayList<user> users = new ArrayList<>();

    private static int idCounter = 0; // id 的唯一性

    public value(String name, valueType valueType, value parent){
        this.id = idCounter++;
        this.name = name;
        this.valueType = valueType;
        this.parent = parent;
    }

    public int getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public valueType getValueType(){
        return valueType;
    }
    public value getParent(){
        return parent;
    }
    public void setParent(value parent){
        this.parent = parent;
    }

    public void setName(int num){
        this.name = "%" + num;
    }
    public ArrayList<user> getUsers(){
        return users;
    }
    /**
     * value 登记 selfUser
     * @param selfUser 当前 value 的使用者
     */
    public void addUser(user selfUser){
        users.add(selfUser);
    }
    /**
     * 移除 user,即不再被 user 使用
     * @param user 使用者
     */
    public void removeUser(user user) {
        if( !users.contains(user) )
            throw new AssertionError("value-" + getId() + " try to remove nonexistent user: " + user + " " + user.getId());
        users.remove(user);
    }

    /**
     * 调用者作为一个被使用者,告诉它的 users,它要被替换成 replacement
     * @param replacement 替代品
     */
    public void selfReplace(value replacement){
        ArrayList<user> usersClone = new ArrayList<>(users);
        for (user user : usersClone){
            for (int i = 0; i < user.getNumOfOps(); i++){
                if (user.getValue(i) == this){
                    user.updateValue(i, replacement);
                }
            }
        }
        users.clear();
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true; // 引用同一个内存地址
        if (o == null || getClass() != o.getClass()) return false;
        value value = (value) o;
        return id == value.id;
    }

    @Override
    public int hashCode(){
        return Objects.hash(id);
    }

}
