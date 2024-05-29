package token;

import java.io.PrintStream;

public class Token {

    public enum tokenType {
        IDENFR, INTCON, STRCON, MAINTK, CONSTTK, INTTK, BREAKTK, CONTINUETK, IFTK, ELSETK,
        NOT, AND, OR, FORTK, GETINTTK, PRINTFTK, RETURNTK, PLUS, MINU, VOIDTK,
        MULT, DIV, MOD, LSS, LEQ, GRE, GEQ, EQL, NEQ,
        ASSIGN, SEMICN, COMMA, LPARENT, RPARENT, LBRACK, RBRACK, LBRACE, RBRACE;

    }
    private final tokenType type;
    private final int lineNum;
    private final String content;

    public Token(tokenType type, int lineNum, String content) {
        this.type = type;
        this.lineNum = lineNum;
        this.content = content;
    }

    //final只允许访问
    public tokenType getType() {
        return type;
    }

    public int getLineNum() {
        return lineNum;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return type.toString() + " " + content + "\n";
    }//重写方法
}
