package error;

public enum ErrorType {
    IllegalSymbol, MultiDefinition, Undefined, ParamNumber, ParamClass, WrongReturn, NoReturn, AssignConst, NoSemi, NoRightSmall, NoRightMiddle, PrintNum, BreakContinue;

    public static String toCode(ErrorType type) {
        if (type == IllegalSymbol) return "a";
        if (type == MultiDefinition) return "b";
        if (type == Undefined) return "c";
        if (type == ParamNumber) return "d";
        if (type == ParamClass) return "e";
        if (type == WrongReturn) return "f";
        if (type == NoReturn) return "g";
        if (type == AssignConst) return "h";
        if (type == NoSemi) return "i";
        if (type == NoRightSmall) return "j";
        if (type == NoRightMiddle) return "k";
        if (type == PrintNum) return "l";
        if (type == BreakContinue) return "m";
        return null;
    }
}
