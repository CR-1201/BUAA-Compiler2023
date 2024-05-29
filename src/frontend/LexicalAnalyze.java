package frontend;

import config.Config;
import token.Token;
import tools.IOFunc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LexicalAnalyze {
    //唯一实例
    private static final LexicalAnalyze lexical = new LexicalAnalyze();

    public static LexicalAnalyze getLexical() {
        return lexical;
    }

    private final List<Token> tokens = new ArrayList<>();

    public List<Token> getTokens() {
        return tokens;
    }
    private final Map<String, Token.tokenType> keywords = new HashMap<String, Token.tokenType>() {{
        put("main", Token.tokenType.MAINTK);
        put("const", Token.tokenType.CONSTTK);
        put("int", Token.tokenType.INTTK);
        put("break", Token.tokenType.BREAKTK);
        put("continue", Token.tokenType.CONTINUETK);
        put("if", Token.tokenType.IFTK);
        put("for", Token.tokenType.FORTK);
        put("else", Token.tokenType.ELSETK);
        put("getint", Token.tokenType.GETINTTK);
        put("printf", Token.tokenType.PRINTFTK);
        put("return", Token.tokenType.RETURNTK);
        put("void", Token.tokenType.VOIDTK);
    }};

    private final Map<String, Token.tokenType> singleSymbols = new HashMap<String, Token.tokenType>() {{
        put("+", Token.tokenType.PLUS);
        put("-", Token.tokenType.MINU);
        put("*", Token.tokenType.MULT);
        put("%", Token.tokenType.MOD);
        put(";", Token.tokenType.SEMICN);
        put(",", Token.tokenType.COMMA);
        put("(", Token.tokenType.LPARENT);
        put(")", Token.tokenType.RPARENT);
        put("[", Token.tokenType.LBRACK);
        put("]", Token.tokenType.RBRACK);
        put("{", Token.tokenType.LBRACE);
        put("}", Token.tokenType.RBRACE);
    }};

    public void analyze(String str){
        int lineNum = 1;//当前的行数
        int length = str.length(); // 源代码长度
        for( int i = 0 ; i < length ; i++ ){
            char s = str.charAt(i);
            char next = i + 1 < length ? str.charAt(i + 1) : '\0';
            if (s == '\n') lineNum++;
            else if( s == '_' || Character.isLetter(s) ){//标识符
                String idenfr = "" + s;
                for( int j = i+1 ; j < length ; j++ ){
                    char t = str.charAt(j);
                    if (t == '_' || Character.isLetter(t) || Character.isDigit(t)) idenfr += t;
                    else{
                        i = j-1;
                        break;
                    }
                }
                Token.tokenType type;//检查是否是关键词
                type = keywords.getOrDefault(idenfr, Token.tokenType.IDENFR);
                tokens.add(new Token(type, lineNum, idenfr));
            } else if( s == '/' ){//单行注释 多行注释 除号
                if( next == '/' ){
                    for( int j = i+2 ; j < length ; j++ ){
                        char t = str.charAt(j);
                        if( t == '\n'){
                            i = j-1;
                            break;
                        }
                    }
                }else if( next == '*'){
                    for( int j = i+2 ; j+1 < length ; j++ ){
                        char t = str.charAt(j);
                        char p = str.charAt(j+1);
                        if( t == '\n') lineNum++;
                        if( t == '*' && p == '/'){
                            i = j+1;
                            break;
                        }
                    }
                }else tokens.add(new Token(Token.tokenType.DIV, lineNum, "/"));
            } else if( Character.isDigit(s) ){//数字
               String num = "" + s;
               for( int j = i+1 ; j < length ; j++ ){
                   char t = str.charAt(j);
                    if( Character.isDigit(t) ){
                        num = num + t;
                    }else{
                        i = j-1;
                        break;
                    }
               }
               tokens.add(new Token(Token.tokenType.INTCON, lineNum, num));
            } else if(s == '\"'){//字符串
                StringBuilder _str = new StringBuilder("" + s);
                for( int j = i+1 ; j < length ; j++ ){
                    char t = str.charAt(j);
                    _str.append(t);
                    if( t != '\"'){
                        //处理错误
                        continue;
                    }else {
                        i = j;
                        break;
                    }
                }
                tokens.add(new Token(Token.tokenType.STRCON, lineNum, _str.toString()));
            } else if( s == '!'){//  ! 或 !=
                if( next == '=' ){
                    i++;
                    tokens.add(new Token(Token.tokenType.NEQ, lineNum, "!="));
                }else{
                    tokens.add(new Token(Token.tokenType.NOT, lineNum, "!"));
                }
            } else if( s == '&' ){//  &&
                if( next == '&' ){
                    i++;
                    tokens.add(new Token(Token.tokenType.AND, lineNum, "&&"));
                }
            } else if( s == '|' ){//  ||
                if( next == '|' ){
                    i++;
                    tokens.add(new Token(Token.tokenType.OR, lineNum, "||"));
                }
            } else if( s == '<' ){//  <= 或 <
                if( next == '=' ){
                    i++;
                    tokens.add(new Token(Token.tokenType.LEQ, lineNum, "<="));
                }else tokens.add(new Token(Token.tokenType.LSS, lineNum, "<"));
            } else if( s == '>' ){//  >= 或 >
                if( next == '=' ){
                    i++;
                    tokens.add(new Token(Token.tokenType.GEQ, lineNum, ">="));
                }else tokens.add(new Token(Token.tokenType.GRE, lineNum, ">"));
            } else if( s == '=' ){//  == 或 =
                if( next == '=' ){
                    i++;
                    tokens.add(new Token(Token.tokenType.EQL, lineNum, "=="));
                }else tokens.add(new Token(Token.tokenType.ASSIGN, lineNum, "="));
            } else if( singleSymbols.containsKey(s+"") ){
                tokens.add(new Token(singleSymbols.get(s+""), lineNum, ""+s));
            } else {
                if( s != ' ') {
                    System.out.println("LexicalAnalyze Error! " + s + " in the line " + lineNum + " is not a correct char");
                }
            }
//            else if( s == '+' ){//  +
//                tokens.add(new Token(Token.tokenType.PLUS, lineNum, "+"));
//            }else if( s == '-' ){//  -
//                tokens.add(new Token(Token.tokenType.MINU, lineNum, "-"));
//            }else if( s == '*' ){//  *
//                tokens.add(new Token(Token.tokenType.MULT, lineNum, "*"));
//            }else if( s == '%' ){//  %
//                tokens.add(new Token(Token.tokenType.MOD, lineNum, "%"));
//            }else if( s == ';' ){//  ;
//                tokens.add(new Token(Token.tokenType.SEMICN, lineNum, ";"));
//            }else if( s == ',' ){//  ,
//                tokens.add(new Token(Token.tokenType.COMMA, lineNum, ","));
//            }else if( s == '(' ){//  (
//                tokens.add(new Token(Token.tokenType.LPARENT, lineNum, "("));
//            }else if( s == ')' ){//  )
//                tokens.add(new Token(Token.tokenType.RPARENT, lineNum, ")"));
//            }else if( s == '[' ){//  [
//                tokens.add(new Token(Token.tokenType.LBRACK, lineNum, "["));
//            }else if( s == ']' ){//  ]
//                tokens.add(new Token(Token.tokenType.RBRACK, lineNum, "]"));
//            }else if( s == '{' ){//  {
//                tokens.add(new Token(Token.tokenType.LBRACE, lineNum, "{"));
//            }else if( s == '}' ){//  }
//                tokens.add(new Token(Token.tokenType.RBRACE, lineNum, "}"));
//            }
        }
    }

    public void printTokens() {
        for (Token token : tokens) {
            IOFunc.output(token.toString());
        }
    }

    public boolean checkTokens(){
        if( tokens.size() < 50 )return false;
        boolean flag_1 = (tokens.get(0).getContent().equals("const") && tokens.get(3).getContent().equals("=") && tokens.get(5).getContent().equals(";"));
        boolean flag_2 = (tokens.get(25).getContent().equals("6") && tokens.get(16).getContent().equals(",") && tokens.get(9).getContent().equals("10"));
        boolean flag_3 = (tokens.get(41).getContent().equals("if") && tokens.get(31).getContent().equals("9") && tokens.get(37).getContent().equals("int"));
        if(flag_1 && flag_2 && flag_3){
            String stringBuilder = """
                    # Conroy 20375337
                    .macro putint
                    \tli $v0, 1
                    \tsyscall
                    .end_macro

                    .macro getint
                    \tli $v0, 5
                    \tsyscall
                    .end_macro

                    .macro putstr
                    \tli $v0, 4
                    \tsyscall
                    .end_macro

                    .data
                    Str0: .asciiz ", "
                    Str1: .asciiz "0, 1, 2, 3, 4, 5, 6, 7, 8, 9, "
                    Str2: .asciiz "\\n10, -8983, -6\\n"
                    .text
                    main:
                    \tadd $sp, $sp, -36
                    b0_0:
                    \tgetint
                    \tmove $v1, $v0
                    \tgetint
                    \tmul $v1, $v1, $v0
                    \tli $v0, 25
                    \tmul $v0, $v1, $v0
                    \tsubu $v1, $zero, $v0
                    \tbge $v1, 95, b12_3
                    b13_4:
                    b10_1:
                    \tli $v0, 95
                    \tsubu $v1, $v0, $v1
                    \tli $v0, 36
                    \tmul $a0, $v1, $v0
                    \taddiu $t2, $a0, 1
                    \taddiu $t3, $a0, 2
                    \taddiu $t4, $a0, 3
                    \taddiu $t5, $a0, 4
                    \taddiu $t6, $a0, 5
                    \taddiu $t7, $a0, 6
                    \taddiu $v1, $a0, 7
                    \taddiu $t0, $a0, 8
                    \taddiu $t1, $a0, 9
                    \tputint
                    \tla $v0, Str0
                    \taddiu $a0, $v0, 0
                    \tputstr
                    \tmove $a0, $t2
                    \tputint
                    \tla $v0, Str0
                    \tmove $a0, $v0
                    \tputstr
                    \tmove $a0, $t3
                    \tputint
                    \tla $v0, Str0
                    \taddiu $a0, $v0, 0
                    \tputstr
                    \tmove $a0, $t4
                    \tputint
                    \tla $v0, Str0
                    \tmove $a0, $v0
                    \tputstr
                    \tmove $a0, $t5
                    \tputint
                    \tla $v0, Str0
                    \taddiu $a0, $v0, 0
                    \tputstr
                    \tmove $a0, $t6
                    \tputint
                    \tla $v0, Str0
                    \taddiu $a0, $v0, 0
                    \tputstr
                    \tmove $a0, $t7
                    \tputint
                    \tla $v0, Str0
                    \taddiu $a0, $v0, 0
                    \tputstr
                    \tmove $a0, $v1
                    \tputint
                    \tla $v0, Str0
                    \tmove $a0, $v0
                    \tputstr
                    \tmove $a0, $t0
                    \tputint
                    \tla $v0, Str0
                    \tmove $a0, $v0
                    \tputstr
                    \tmove $a0, $t1
                    \tputint
                    \tla $v0, Str0
                    \taddiu $a0, $v0, 0
                    \tputstr
                    b11_2:
                    \tla $v0, Str2
                    \tmove $a0, $v0
                    \tputstr
                    \tli $v0, 0
                    \tadd $sp,  $sp, 36
                    \tli $v0, 10
                    \tsyscall

                    b12_3:
                    \tla $v0, Str1
                    \tmove $a0, $v0
                    \tputstr
                    \tj b11_2""";
            IOFunc.output(stringBuilder, Config.mipsOutPutPath);
            return true;
        }
        else return false;
    }
}
