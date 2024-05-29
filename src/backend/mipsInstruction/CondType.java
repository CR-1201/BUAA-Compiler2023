package backend.mipsInstruction;

import ir.instructions.Binary_Instructions.ICMP.*;

public enum CondType {
    ANY, EQ, NE, GE, GT, LE, LT;
    public static CondType genCond(Condition irCondition) {
        switch (irCondition) {
            case EQ -> {
                return CondType.EQ;
            }
            case LE -> {
                return CondType.LE;
            }
            case LT -> {
                return CondType.LT;
            }
            case GE -> {
                return CondType.GE;
            }
            case GT -> {
                return CondType.GT;
            }
            case NE -> {
                return CondType.NE;
            }
            default -> {
                return CondType.ANY;
            }
        }
    }
    public static CondType getEqualOppCond(CondType type) {
        switch (type) {
            case EQ -> {
                return CondType.EQ;
            }
            case LE -> {
                return CondType.GE;
            }
            case LT -> {
                return CondType.GT;
            }
            case GE -> {
                return CondType.LE;
            }
            case GT -> {
                return CondType.LT;
            }
            case NE -> {
                return CondType.NE;
            }
            default -> {
                return CondType.ANY;
            }
        }
    }
    public static CondType getOppCond(CondType type) {
        switch (type) {
            case EQ -> {
                return NE;
            }
            case LE -> {
                return GT;
            }
            case LT -> {
                return GE;
            }
            case GE -> {
                return LT;
            }
            case GT -> {
                return LE;
            }
            case NE -> {
                return EQ;
            }
            default -> {
                assert false;
                return CondType.ANY;
            }
        }
    }
    public boolean compare(int op1, int op2) {
        switch (this) {
            case EQ -> {
                return op1 == op2;
            }
            case NE -> {
                return op1 != op2;
            }
            case GE -> {
                return op1 >= op2;
            }
            case GT -> {
                return op1 > op2;
            }
            case LE -> {
                return op1 <= op2;
            }
            case LT -> {
                return op1 < op2;
            }
            default -> {
                return false;
            }
        }
    }
    @Override
    public String toString() {
        switch (this) {
            case EQ -> {
                return "eq";
            }
            case NE -> {
                return "ne";
            }
            case GE -> {
                return "ge";
            }
            case GT -> {
                return "gt";
            }
            case LE -> {
                return "le";
            }
            case LT -> {
                return "lt";
            }
            default -> {
                return "";
            }
        }
    }
}

