package frontend;

import error.ErrorType;
import error.SyntaxError;
import node.*;
import token.Token;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;


public class ParserAnalyze {
    // 唯一实例
    private static final ParserAnalyze parser = new ParserAnalyze();

    private  SyntaxError syntaxError = new SyntaxError();
    public static ParserAnalyze getParser() {
        return parser;
    }

    private List<Token> tokens;
    private int index = 0;

    private CompUnit compUnit;

    public CompUnit getCompUnit() {
        return compUnit;
    }

    public void analyze() {
        this.compUnit = CompUnit();
    }

    PrintStream ps;

    {
        try {
            ps = new PrintStream("output.txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Token currentToken;

    public void setTokens(List<Token> tokens) {
        this.tokens = tokens;
        this.currentToken = tokens.get(index);
    }

    private CompUnit CompUnit() {
        // CompUnit -> {Decl} {FuncDef} MainFuncDef
        List<Decl> decl = new ArrayList<>();
        List<FuncDef> funcDef = new ArrayList<>();
        MainFuncDef mainFuncDef;
        while (index < tokens.size() - 2 && tokens.get(index + 1).getType() != Token.tokenType.MAINTK && tokens.get(index + 2).getType() != Token.tokenType.LPARENT) {
            Decl declNode = Decl();
            decl.add(declNode);
        }
        while (index < tokens.size() - 1 && tokens.get(index + 1).getType() != Token.tokenType.MAINTK) {
            FuncDef funcDefNode = FuncDef();
            funcDef.add(funcDefNode);
        }
        mainFuncDef = MainFuncDef();
        return new CompUnit(decl, funcDef, mainFuncDef);
    }
    private Decl Decl() {
        // Decl -> ConstDecl | VarDecl
        ConstDecl constDecl = null;
        VarDecl varDecl = null;
        if (currentToken.getType() == Token.tokenType.CONSTTK) {
            constDecl = ConstDecl();
        } else {
            varDecl = VarDecl();
        }
        return new Decl(constDecl, varDecl);
    }

    private ConstDecl ConstDecl() {
        // ConstDecl -> 'const' BType ConstDef { ',' ConstDef } ';'
        Token constToken = match(Token.tokenType.CONSTTK);
        BType bType= BType();
        List<ConstDef> constDef = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        Token semicnToken;
        constDef.add(ConstDef());
        while (currentToken.getType() == Token.tokenType.COMMA) {
            commas.add(match(Token.tokenType.COMMA));
            constDef.add(ConstDef());
        }
        semicnToken = match(Token.tokenType.SEMICN);
        return new ConstDecl(constToken, bType, constDef, commas, semicnToken);
    }

    private ConstDef ConstDef(){
        // ConstDef -> Ident { '[' ConstExp ']' } '=' ConstInitVal
        Token identToken = match(Token.tokenType.IDENFR);
        List<Token> lbrackToken = new ArrayList<>();
        List<ConstExp> constExp = new ArrayList<>();
        List<Token> rbrackToken = new ArrayList<>();
        Token assignToken;
        ConstInitVal constInitVal;
        while (currentToken.getType() != Token.tokenType.ASSIGN) {
            lbrackToken.add(match(Token.tokenType.LBRACK));
            constExp.add(ConstExp());
            rbrackToken.add(match(Token.tokenType.RBRACK));
        }
        assignToken = match(Token.tokenType.ASSIGN);
        constInitVal = ConstInitVal();
        return new ConstDef(identToken,lbrackToken,constExp,rbrackToken,assignToken,constInitVal);
    }

    private ConstExp ConstExp(){
        // ConstExp → AddExp
        AddExp addExp = AddExp();
        return new ConstExp(addExp);
    }

    private AddExp AddExp(){
        // AddExp → MulExp | AddExp ('+' | '−') MulExp
        List<MulExp> mulExps = new ArrayList<>();
        List<Token> addExpTKs = new ArrayList<>();
        mulExps.add(MulExp());
        while( currentToken.getType() == Token.tokenType.PLUS || currentToken.getType() == Token.tokenType.MINU ){
            if( currentToken.getType() == Token.tokenType.PLUS ){
                addExpTKs.add(match(Token.tokenType.PLUS));
            }else if( currentToken.getType() == Token.tokenType.MINU ){
                addExpTKs.add(match(Token.tokenType.MINU));
            }
            mulExps.add(MulExp());
        }
        return new AddExp(mulExps,addExpTKs);
    }

    private MulExp MulExp(){
        //  MulExp → UnaryExp | MulExp ('*' | '/' | '%') UnaryExp
        List<UnaryExp> unaryExps = new ArrayList<>();
        List<Token> mulExpTKs = new ArrayList<>();
        unaryExps.add(UnaryExp());
        while(currentToken.getType() == Token.tokenType.MULT || currentToken.getType() == Token.tokenType.DIV || currentToken.getType() == Token.tokenType.MOD){
            if( currentToken.getType() == Token.tokenType.MULT ){
                mulExpTKs.add(match(Token.tokenType.MULT));
            }else if( currentToken.getType() == Token.tokenType.DIV ){
                mulExpTKs.add(match(Token.tokenType.DIV));
            }else if( currentToken.getType() == Token.tokenType.MOD ){
                mulExpTKs.add(match(Token.tokenType.MOD));
            }
            unaryExps.add(UnaryExp());
        }
        return new MulExp(unaryExps,mulExpTKs);
    }

    private RelExp RelExp(){
        // RelExp → AddExp | RelExp ('<' | '>' | '<=' | '>=') AddExp
        List<AddExp> addExps = new ArrayList<>();
        List<Token> relExpTKs = new ArrayList<>();
        addExps.add(AddExp());
        while( currentToken.getType() == Token.tokenType.LSS || currentToken.getType() == Token.tokenType.LEQ
        || currentToken.getType() == Token.tokenType.GRE || currentToken.getType() == Token.tokenType.GEQ ){
            if( currentToken.getType() == Token.tokenType.LSS ){
                relExpTKs.add(match(Token.tokenType.LSS));
            }else if( currentToken.getType() == Token.tokenType.LEQ ){
                relExpTKs.add(match(Token.tokenType.LEQ));
            }else if( currentToken.getType() == Token.tokenType.GRE ){
                relExpTKs.add(match(Token.tokenType.GRE));
            }else if( currentToken.getType() == Token.tokenType.GEQ ){
                relExpTKs.add(match(Token.tokenType.GEQ));
            }
            addExps.add(AddExp());
        }
        return new RelExp(addExps,relExpTKs);
    }

    private EqExp EqExp(){
        // EqExp → RelExp | EqExp ('==' | '!=') RelExp
        List<RelExp> relExps = new ArrayList<>();
        List<Token> eqExpTKs = new ArrayList<>();
        relExps.add(RelExp());
        while( currentToken.getType() == Token.tokenType.EQL || currentToken.getType() == Token.tokenType.NEQ ){
            if( currentToken.getType() == Token.tokenType.EQL ){
                eqExpTKs.add(match(Token.tokenType.EQL));
            }else if( currentToken.getType() == Token.tokenType.NEQ ){
                eqExpTKs.add(match(Token.tokenType.NEQ));
            }
            relExps.add(RelExp());
        }
        return new EqExp(relExps,eqExpTKs);
    }

    private LAndExp LAndExp(){
        // LAndExp → EqExp | LAndExp '&&' EqExp
        List<EqExp> eqExps = new ArrayList<>();
        List<Token> lAndExpTKs = new ArrayList<>();
        eqExps.add(EqExp());
        while( currentToken.getType() == Token.tokenType.AND ){
            lAndExpTKs.add(match(Token.tokenType.AND));
            eqExps.add(EqExp());
        }
        return new LAndExp(eqExps,lAndExpTKs);
    }

    private  ConstInitVal ConstInitVal(){
        // ConstInitVal → ConstExp | '{' [ ConstInitVal { ',' ConstInitVal } ] '}'
        ConstExp constExp = null;
        Token lbrace = null;
        List<ConstInitVal> constInitVals = new ArrayList<>();
        Token rbrace = null;
        List<Token> commas = new ArrayList<>();
        if( currentToken.getType() == Token.tokenType.LBRACE ){
            lbrace = match(Token.tokenType.LBRACE);
            if(currentToken.getType() != Token.tokenType.RBRACE ) { // constInitVals不为空
                constInitVals.add(ConstInitVal());
                while(currentToken.getType() == Token.tokenType.COMMA) {
                    commas.add(match(Token.tokenType.COMMA));
                    constInitVals.add(ConstInitVal());
                }
            }
            rbrace = match(Token.tokenType.RBRACE);
            return new ConstInitVal(lbrace,constInitVals,rbrace,commas);
        }else {
            constExp = ConstExp();
            return new ConstInitVal(constExp);
        }
    }
    private BType BType(){
        // BType -> 'int'
        Token bTypeToken = match(Token.tokenType.INTTK);
        return new BType(bTypeToken);
    }
    private VarDecl VarDecl(){
        // VarDecl → BType VarDef { ',' VarDef } ';'
        BType bType = BType();
        List<VarDef> varDefs = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        Token semicn = null;
        varDefs.add(VarDef());
        while( currentToken.getType() == Token.tokenType.COMMA ){
              commas.add(match(Token.tokenType.COMMA));
              varDefs.add(VarDef());
        }
        semicn = match(Token.tokenType.SEMICN);
        return new VarDecl(bType,varDefs,commas,semicn);
    }

    private VarDef VarDef(){
        // VarDef → Ident { '[' ConstExp ']' } | Ident { '[' ConstExp ']' } '=' InitVal
        Token ident = match(Token.tokenType.IDENFR);
        List<Token> lbracks = new ArrayList<>();
        List<ConstExp> constExps = new ArrayList<>();
        List<Token> rbracks = new ArrayList<>();
        Token assign = null;
        InitVal initVal = null;
        while( currentToken.getType() == Token.tokenType.LBRACK ){
               lbracks.add(match(Token.tokenType.LBRACK));
               constExps.add(ConstExp());
               rbracks.add(match(Token.tokenType.RBRACK));
        }

        if( currentToken.getType() == Token.tokenType.ASSIGN ){
            assign = match(Token.tokenType.ASSIGN);
            initVal = InitVal();
            return new VarDef(ident,lbracks,constExps,rbracks,assign,initVal);
        } 
        else return new VarDef(ident,lbracks,constExps,rbracks);
    }

    private InitVal InitVal(){
        // InitVal → Exp | '{' [ InitVal { ',' InitVal } ] '}'
        Exp exp = null;
        Token lbrace = null;
        List<InitVal> initVals = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        Token rbrace = null;
        if( currentToken.getType() == Token.tokenType.LBRACE ){
               lbrace = match(Token.tokenType.LBRACE);
               initVals.add(InitVal());
               while( currentToken.getType() == Token.tokenType.COMMA ){
                        commas.add(match(Token.tokenType.COMMA));
                        initVals.add(InitVal());
               }
               rbrace= match(Token.tokenType.RBRACE);
               return new InitVal(lbrace,initVals,commas,rbrace);
        }else{
               exp = Exp();
               return new InitVal(exp);
        }
    }

    private Exp Exp(){
        // Exp → AddExp
        AddExp addExp = AddExp();
        return new Exp(addExp);
    }
    private FuncDef FuncDef(){
        // FuncDef → FuncType Ident '(' [FuncFParams] ')' Block
        // FuncFParams → FuncFParam { ',' FuncFParam }
        FuncType funcType = FuncType();
        Token ident = match(Token.tokenType.IDENFR);
        Token lparent = match(Token.tokenType.LPARENT);
        List<FuncFParam> funcFParams = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        Token rparent = null;
        Block block = null;
        if( currentToken.getType() != Token.tokenType.RPARENT && currentToken.getType() == Token.tokenType.INTTK){
           funcFParams.add(FuncFParam());
           while( currentToken.getType() == Token.tokenType.COMMA ){
               commas.add(match(Token.tokenType.COMMA));
               funcFParams.add(FuncFParam());
           }
        }
        rparent =  match(Token.tokenType.RPARENT);
        block = Block();
        return new FuncDef(funcType,ident,lparent,funcFParams,commas,rparent,block);
    }

    private Block Block(){
        // Block → '{' { BlockItem } '}'
        Token lbrace = match(Token.tokenType.LBRACE);
        List<BlockItem> blockItems = new ArrayList<>();
        Token rbrace = null;
        while( currentToken.getType() != Token.tokenType.RBRACE ){
            blockItems.add(BlockItem());
        }
        rbrace = match(Token.tokenType.RBRACE);    
        return new Block(lbrace,blockItems,rbrace);
    }

    private BlockItem  BlockItem(){
        // BlockItem → Decl | Stmt
        Decl decl;
        Stmt stmt;
        if( currentToken.getType() == Token.tokenType.INTTK || currentToken.getType() == Token.tokenType.CONSTTK){
            decl =  Decl();
            return new BlockItem(decl);
        }else{
            stmt = Stmt();
            return new BlockItem(stmt);
        }
    }

    private Stmt Stmt(){
//         Stmt → LVal '=' Exp ';' // 每种类型的语句都要覆盖
//         | [Exp] ';' //有无Exp两种情况
//         | Block
//         | 'if' '(' Cond ')' Stmt [ 'else' Stmt ] // 1.有else 2.无else
//         | 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
//         | 'break' ';' | 'continue' ';'
//         | 'return' [Exp] ';' // 1.有Exp 2.无Exp
//         | LVal '=' 'getint''('')'';'
//         | 'printf''('FormatString{','Exp}')'';' // 1.有Exp 2.无Exp
        int type = getStetType();
        LVal lVal = null;
        Token assignTK = null;
        Exp exp = null;
        Token semicnTK = null;
        Block block = null;
        Token ifTK = null;
        Token lparentTK = null;
        Cond cond = null;
        Token rparentTK = null;
        Stmt stmt_1 = null;
        Token elseTK = null;
        Stmt stmt_2 = null;
        Token forTk = null;
        ForStmt forStmt_1 = null;
        ForStmt forStmt_2 = null;
        List<Token> semicnTKs = new ArrayList<>();
        Token breakTK = null;
        Token continueTK = null;
        Token returnTK = null;
        Token getintTK = null;
        Token printfTK = null;
        Token formatString = null;
        List<Token> commas = new ArrayList<>();
        List<Exp> exps = new ArrayList<>();
        switch (type){
            case 1:
                lVal = LVal();
                assignTK = match(Token.tokenType.ASSIGN);
                exp = Exp();
                semicnTK = match(Token.tokenType.SEMICN);
                return new Stmt(type,lVal,assignTK,exp,semicnTK);
            case 2:
                if( currentToken.getType() != Token.tokenType.SEMICN ){
                    exp = Exp();
                }
                semicnTK = match(Token.tokenType.SEMICN);
                return new Stmt(type,exp,semicnTK);
            case 3:
                block = Block();
                return new Stmt(type,block);
            case 4:
                ifTK = match(Token.tokenType.IFTK);
                lparentTK = match(Token.tokenType.LPARENT);
                cond =  Cond();
                rparentTK = match(Token.tokenType.RPARENT);
                stmt_1 = Stmt();
                if( currentToken.getType() == Token.tokenType.ELSETK ){
                    elseTK = match(Token.tokenType.ELSETK);
                    stmt_2 = Stmt();
                }
                return new Stmt(type,ifTK,lparentTK,cond,rparentTK,stmt_1,elseTK,stmt_2);
            case 5:
                forTk = match(Token.tokenType.FORTK);
                lparentTK = match(Token.tokenType.LPARENT);
                if( currentToken.getType() != Token.tokenType.SEMICN){
                    forStmt_1 = ForStmt();
                }
                semicnTKs.add(match(Token.tokenType.SEMICN));
                if( currentToken.getType() != Token.tokenType.SEMICN){
                    cond =  Cond();
                }
                semicnTKs.add(match(Token.tokenType.SEMICN));
                if( currentToken.getType() != Token.tokenType.RPARENT){
                    forStmt_2 = ForStmt();
                }
                rparentTK = match(Token.tokenType.RPARENT);
                stmt_1 = Stmt();
                return new Stmt(type,forTk,lparentTK,forStmt_1,forStmt_2,semicnTKs,cond,rparentTK,stmt_1);
            case 6:
                if( currentToken.getType() == Token.tokenType.CONTINUETK ){
                    continueTK = match(Token.tokenType.CONTINUETK);
                    semicnTK = match(Token.tokenType.SEMICN);
                    return new Stmt(type,continueTK,semicnTK);
                }else{
                    breakTK = match(Token.tokenType.BREAKTK);
                    semicnTK = match(Token.tokenType.SEMICN);
                    return new Stmt(type,breakTK,semicnTK);
                }
            case 7:
                returnTK = match(Token.tokenType.RETURNTK);
                if(currentToken.getType() != Token.tokenType.SEMICN){
                    exp = Exp();
                }
                semicnTK = match(Token.tokenType.SEMICN);
                return new Stmt(type,returnTK,exp,semicnTK);
            case 8:
                lVal = LVal();
                assignTK = match(Token.tokenType.ASSIGN);
                getintTK = match(Token.tokenType.GETINTTK);
                lparentTK = match(Token.tokenType.LPARENT);
                rparentTK = match(Token.tokenType.RPARENT);
                semicnTK = match(Token.tokenType.SEMICN);
                return new Stmt(type,lVal,assignTK,getintTK,lparentTK,rparentTK,semicnTK);
            case 9:
                printfTK = match(Token.tokenType.PRINTFTK);
                lparentTK = match(Token.tokenType.LPARENT);
                formatString = match(Token.tokenType.STRCON);
                while(currentToken.getType() == Token.tokenType.COMMA){
                    commas.add(match(Token.tokenType.COMMA));
                    exps.add(Exp());
                }
                rparentTK = match(Token.tokenType.RPARENT);
                semicnTK = match(Token.tokenType.SEMICN);
                return new Stmt(type,printfTK,lparentTK,formatString,commas,exps,rparentTK,semicnTK);
        }
        return null;
    }

    private int getStetType(){
        int type = 0;
        if( currentToken.getType() == Token.tokenType.LBRACE ){
            type = 3;
        }else if( currentToken.getType() == Token.tokenType.IFTK ){
            type = 4;
        }else if( currentToken.getType() == Token.tokenType.FORTK ){
            type = 5;
        }else if( currentToken.getType() == Token.tokenType.BREAKTK || currentToken.getType() == Token.tokenType.CONTINUETK){
            type = 6;
        }else if( currentToken.getType() == Token.tokenType.RETURNTK ){
            type = 7;
        }else if( currentToken.getType() == Token.tokenType.PRINTFTK ){
            type = 9;
        }
        if(type!=0)return type;
        int len = index;
        int flag = 0;
        while( tokens.get(len).getType() != Token.tokenType.SEMICN ){
            if( tokens.get(len).getType() == Token.tokenType.ASSIGN ){
                flag = 1;
            }
            if( tokens.get(len).getType() == Token.tokenType.GETINTTK ){
                type = 8;
                break;
            }
            len++;
        }
        if( type == 0 ){
            if( flag == 1 ){
                type = 1;
            }else
                type = 2;
        }
        return type;
    }

    private LVal LVal(){
        //  LVal → Ident {'[' Exp ']'}
        Token ident = match(Token.tokenType.IDENFR);
        List<Token> lbrackTKs = new ArrayList<>();
        List<Exp> exps = new ArrayList<>();
        List<Token> rbrackTKs = new ArrayList<>();
        while(currentToken.getType() == Token.tokenType.LBRACK){
            lbrackTKs.add(match(Token.tokenType.LBRACK));
            exps.add(Exp());
            rbrackTKs.add(match(Token.tokenType.RBRACK));
        }
        return new LVal(ident,lbrackTKs,exps,rbrackTKs);
    }

    private PrimaryExp PrimaryExp(){
        // PrimaryExp → '(' Exp ')' | LVal | Number
        //  Number → IntConst
        if( currentToken.getType() == Token.tokenType.LPARENT ){
            Token lparentTK = match(Token.tokenType.LPARENT);
            Exp exp = Exp();
            Token rparentTK = match(Token.tokenType.RPARENT);
            return new PrimaryExp(lparentTK,exp,rparentTK);
        }else if( currentToken.getType() == Token.tokenType.INTCON ){
            IntConstNum intConst = IntConstNum();
            return new PrimaryExp(intConst);
        }else{
            LVal lVal = LVal();
            return new PrimaryExp(lVal);
        }
    }

    private UnaryExp UnaryExp(){
        // UnaryExp → PrimaryExp | Ident '(' [FuncRParams] ')' | UnaryOp UnaryExp
        FuncRParams funcRParams = null;
        if(currentToken.getType() == Token.tokenType.IDENFR && index < tokens.size() - 1 && tokens.get(index+1).getType() == Token.tokenType.LPARENT){
            Token identTK = match(Token.tokenType.IDENFR);
            Token lparentTK = match(Token.tokenType.LPARENT);
            if(currentToken.getType() != Token.tokenType.RPARENT && isExp()){
                funcRParams = FuncRParams();
            }
            Token rparentTK = match(Token.tokenType.RPARENT);
            return new UnaryExp(identTK,lparentTK,funcRParams,rparentTK);
        }
        else if( currentToken.getType() == Token.tokenType.PLUS || currentToken.getType() == Token.tokenType.MINU || currentToken.getType() == Token.tokenType.NOT){
            UnaryOp unaryOp = UnaryOp();
            UnaryExp unaryExp = UnaryExp();
            return new UnaryExp(unaryOp,unaryExp);
        }
        else{
            PrimaryExp primaryExp = PrimaryExp();
            return new UnaryExp(primaryExp);
        }
    }

    private boolean isExp() {
        return  currentToken.getType() == Token.tokenType.IDENFR ||
                currentToken.getType() == Token.tokenType.PLUS ||
                currentToken.getType() == Token.tokenType.MINU ||
                currentToken.getType() == Token.tokenType.NOT ||
                currentToken.getType() == Token.tokenType.LPARENT ||
                currentToken.getType() == Token.tokenType.INTCON;
    }
    private FuncRParams FuncRParams(){
        //  FuncRParams → Exp { ',' Exp }
        List<Exp> exps = new ArrayList<>();
        List<Token> commas = new ArrayList<>();
        exps.add(Exp());
        while(currentToken.getType()==Token.tokenType.COMMA){
            commas.add(match(Token.tokenType.COMMA));
            exps.add(Exp());
        }
        return new FuncRParams(exps,commas);
    }

    private UnaryOp UnaryOp(){
        //  UnaryOp → '+' | '−' | '!'
        Token unaryOp = null;
        if( currentToken.getType() == Token.tokenType.PLUS ){
            unaryOp = match(Token.tokenType.PLUS);
        }else if( currentToken.getType() == Token.tokenType.MINU ){
            unaryOp = match(Token.tokenType.MINU);
        }else if( currentToken.getType() == Token.tokenType.NOT ){
            unaryOp = match(Token.tokenType.NOT);
        }
        return new UnaryOp(unaryOp);
    }
    private IntConstNum IntConstNum(){
        Token num = match(Token.tokenType.INTCON);
        return new IntConstNum(num);
    }
    private Cond Cond(){
        // Cond → LOrExp
        LOrExp lOrExp = LOrExp();
        return new Cond(lOrExp);
    }

    private LOrExp LOrExp(){
        // LOrExp → LAndExp | LOrExp '||' LAndExp
        List<LAndExp> lAndExps = new ArrayList<>();
        List<Token> lOrExpTKs = new ArrayList<>();
        lAndExps.add(LAndExp());
        while( currentToken.getType() == Token.tokenType.OR ){
            lOrExpTKs.add(match(Token.tokenType.OR));
            lAndExps.add(LAndExp());
        }
        return new LOrExp(lAndExps,lOrExpTKs);
    }

    private ForStmt ForStmt(){
        // ForStmt → LVal '=' Exp
        LVal lVal = LVal();
        Token assignTK = match(Token.tokenType.ASSIGN);
        Exp exp = Exp();
        return new ForStmt(lVal,assignTK,exp);
    }
    private FuncFParam FuncFParam(){
        // FuncFParam → BType Ident ['[' ']' { '[' ConstExp ']' }]
        BType bType = BType();
        Token ident = match(Token.tokenType.IDENFR);
        Token lbrack = null;
        Token rbrack = null;
        List<Token> lbracks = new ArrayList<>();
        List<ConstExp> constExps = new ArrayList<>();
        List<Token> rbracks = new ArrayList<>();
        if(currentToken.getType() == Token.tokenType.LBRACK){
              lbrack = match(Token.tokenType.LBRACK);
              rbrack = match(Token.tokenType.RBRACK);
              while(currentToken.getType() == Token.tokenType.LBRACK) {
                    lbracks.add(match(Token.tokenType.LBRACK));
                    constExps.add(ConstExp());
                    rbracks.add(match(Token.tokenType.RBRACK));
              }
        }
        return new FuncFParam(bType,ident,lbrack,rbrack,lbracks,constExps,rbracks);
    }
    private FuncType FuncType(){
        // FuncType → 'void' | 'int'
        Token funcType = null;
        if( currentToken.getType() == Token.tokenType.INTTK) {
               funcType = match(Token.tokenType.INTTK);
        }else if(currentToken.getType() == Token.tokenType.VOIDTK){
               funcType = match(Token.tokenType.VOIDTK);
        }
        return new FuncType(funcType);
    }

    private  MainFuncDef MainFuncDef(){
        // MainFuncDef → 'int' 'main' '(' ')' Block
        Token intTK = match(Token.tokenType.INTTK);
        Token mainTK = match(Token.tokenType.MAINTK);
        Token lparent = match(Token.tokenType.LPARENT);
        Token rparent = match(Token.tokenType.RPARENT);
        Block block = Block();
        return new MainFuncDef(intTK,mainTK,lparent,rparent,block);
    }

    private Token match(Token.tokenType type) {
        if (currentToken.getType() == type) {
            Token tmp = currentToken;
            if (index < tokens.size() - 1) {
                currentToken = tokens.get(++index);
            }
            return tmp;
        } else if (type == Token.tokenType.SEMICN) {
            syntaxError.addError(ErrorType.NoSemi, tokens.get(index - 1).getLineNum());
//            ErrorHandler.getErrorHandler().addError(new Error(tokens.get(index - 1).getLineNum(), ErrorType.i));
            return new Token(Token.tokenType.SEMICN, tokens.get(index - 1).getLineNum(), ";");
        } else if (type == Token.tokenType.RPARENT) {
            syntaxError.addError(ErrorType.NoRightSmall, tokens.get(index - 1).getLineNum());
//            ErrorHandler.getErrorHandler().addError(new Error(tokens.get(index - 1).getLineNum(), ErrorType.j));
            return new Token(Token.tokenType.RPARENT, tokens.get(index - 1).getLineNum(), ")");
        } else if (type == Token.tokenType.RBRACK) {
            syntaxError.addError(ErrorType.NoRightMiddle, tokens.get(index - 1).getLineNum());
//            ErrorHandler.getErrorHandler().addError(new Error(tokens.get(index - 1).getLineNum(), ErrorType.k));
            return new Token(Token.tokenType.RBRACK, tokens.get(index - 1).getLineNum(), "]");
        } else {
            throw new RuntimeException("Syntax error at line " + currentToken.getLineNum() + ": " + currentToken.getContent() + " is not " + type);
        }
    }

    public void printParseAns() {
        compUnit.output(ps);
    }

    public SyntaxError getErrorList() {
        return syntaxError;
    }
}
