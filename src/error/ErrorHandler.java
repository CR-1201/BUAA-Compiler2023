package error;

import node.*;
import symbol.Symbol;
import symbol.SymbolTable;
import token.Token;

import javax.swing.plaf.PanelUI;
import java.util.ArrayList;

public class ErrorHandler {
    public CompUnit syntaxTreeRoot; // 语法树根
    public SymbolTable currentSymbolTable = new SymbolTable(null); //

    public SyntaxError syntaxErrorList;

    public static int loopCount = 0;
    public String getErrorList() {
        travelSyntaxTree(syntaxTreeRoot);
        return syntaxErrorList.toString();
    }

    public ErrorHandler(CompUnit treeRoot, SyntaxError syntaxErrorList) {
        this.syntaxTreeRoot = treeRoot;
        this.syntaxErrorList = new SyntaxError();
        this.syntaxErrorList.errors.addAll(syntaxErrorList.errors);
    }

    public void travelSyntaxTree(FatherNode node){
        if( node != null ){
            for (FatherNode syntaxNode : node.childrenNode){
                if (syntaxNode instanceof ConstDef constDef) {
                    if (currentSymbolTable.isDuplicateCurField(constDef.getIdent()))
                        syntaxErrorList.addError(ErrorType.MultiDefinition, constDef.getIdent().getLineNum());
                    else
                        currentSymbolTable.addSymbol(constDef.getIdent(), true, constDef.getConstInitVal(), null, constDef.getConstExps().size());
                } else if (syntaxNode instanceof VarDef varDef) {
                    if (currentSymbolTable.isDuplicateCurField(varDef.getIdent()))
                        syntaxErrorList.addError(ErrorType.MultiDefinition, varDef.getIdent().getLineNum());
                    else
                        currentSymbolTable.addSymbol(varDef.getIdent(), false, null, varDef.getInitVal(), varDef.getConstExps().size());
                } else if (syntaxNode instanceof FuncFParam funcFParam) {
                    if (currentSymbolTable.isDuplicateCurField(funcFParam.getIdent()))
                        syntaxErrorList.addError(ErrorType.MultiDefinition, funcFParam.getIdent().getLineNum());
                    else currentSymbolTable.addSymbol(funcFParam.getIdent(), funcFParam.getVarType());
                } else if (syntaxNode instanceof FuncDef || syntaxNode instanceof MainFuncDef) {
                    if (syntaxNode instanceof FuncDef funcDef) {
                        if (currentSymbolTable.isDuplicateCurField(funcDef.getIdent()))
                            syntaxErrorList.addError(ErrorType.MultiDefinition, funcDef.getIdent().getLineNum());
                        else
                            currentSymbolTable.addSymbol(funcDef.getIdent(), funcDef.getFuncType(), funcDef.getFuncFParams().size(), funcDef.getFuncFParams());
                        if (funcDef.getFuncType().getType().getContent().equals("int") && !funcDef.isReturn()) {
                            syntaxErrorList.addError(ErrorType.NoReturn, funcDef.getEndLine());
                        }
                    }
                    if (syntaxNode instanceof MainFuncDef mainFuncDef) {
                        if (currentSymbolTable.isDuplicateCurField(mainFuncDef.getIdent()))
                            syntaxErrorList.addError(ErrorType.MultiDefinition, mainFuncDef.getIdent().getLineNum());
                        else currentSymbolTable.addSymbol(mainFuncDef.getIdent(), new FuncType(mainFuncDef.getIntTK()), 0, null);
                        if (!mainFuncDef.isReturn()) {
                            syntaxErrorList.addError(ErrorType.NoReturn, mainFuncDef.getEndLine());
                        }

                    }
                    currentSymbolTable = currentSymbolTable.newSon();
                    travelSyntaxTree(syntaxNode);
                    currentSymbolTable = currentSymbolTable.back();
                    continue;
                } else if (syntaxNode instanceof Stmt stmt) {
                    if (stmt.getType() == 3) {
                        // Block
                        currentSymbolTable = currentSymbolTable.newSon();
                        travelSyntaxTree(syntaxNode);
                        currentSymbolTable = currentSymbolTable.back();
                        continue;
                    } else if (stmt.getType() == 7) {
                        // 'return' [Exp] ';'
                        int size = (stmt.getExp() == null ? 0 : 1);
//                        System.out.println("Return :"+size);
                        if (!currentSymbolTable.checkFatherFuncType(size)) {
                            syntaxErrorList.addError(ErrorType.WrongReturn, stmt.getReturnTK().getLineNum());
                        }
                    } else if (stmt.getType() == 9) {
                        // 'printf''('FormatString{','Exp}')'';'
                        if(!stmt.formatStringCheck()){
                            syntaxErrorList.addError(ErrorType.IllegalSymbol, stmt.getFormatString().getLineNum());
                        }
                        if (stmt.getExps().size() != stmt.formatStringParamNum()) {
                            syntaxErrorList.addError(ErrorType.PrintNum, stmt.getPrintfToken().getLineNum());
                        }
                    } else if (stmt.getType() == 1 || stmt.getType() == 8) {
                        if (currentSymbolTable.checkIsConst(stmt.getLVal().getIdent())) {
                            syntaxErrorList.addError(ErrorType.AssignConst, stmt.getLVal().getIdent().getLineNum());
                        }
                    } else if( stmt.getType() == 5 ){
                        // 'for' '(' [ForStmt] ';' [Cond] ';' [forStmt] ')' Stmt
                        loopCount++;
                        currentSymbolTable = currentSymbolTable.newSon();
                        travelSyntaxTree(syntaxNode);
                        currentSymbolTable = currentSymbolTable.back();
                        loopCount--;
                        continue;
                    } else if(stmt.getType() == 6){
                        // 'break' ';' | 'continue' ';'
                        if( loopCount == 0 ){
                            syntaxErrorList.addError(ErrorType.BreakContinue, stmt.getToken().getLineNum());
                        }
                    }
                } else if (syntaxNode instanceof LVal lVal) {
                    if (currentSymbolTable.isExistUpField(lVal.getIdent()) == null)
                        syntaxErrorList.addError(ErrorType.Undefined, lVal.getIdent().getLineNum());
                } else if (syntaxNode instanceof UnaryExp unaryExp) {
                    // 调用函数
                    if (unaryExp.getIdent() != null) {
                        Symbol symbol = currentSymbolTable.isExistUpField(unaryExp.getIdent());
                        if (symbol == null) {
                            syntaxErrorList.addError(ErrorType.Undefined, unaryExp.getIdent().getLineNum());
                        } else if (unaryExp.getNumOfParam() != symbol.paramsNum) {
                            syntaxErrorList.addError(ErrorType.ParamNumber, unaryExp.getIdent().getLineNum());
                        } else {
                            if (unaryExp.getFuncRParams() != null) {
                                ArrayList<Symbol.Type> types = unaryExp.getFuncRParams().getParamType(currentSymbolTable);
//                                System.out.println("LineNum "+unaryExp.getIdent().getLineNum()+" "+unaryExp.getIdent().getContent()+" types:"+types);
                                for (int i = 0; i < types.size(); i++) {
                                    if (!symbol.funcFParams.get(i).checkType(types.get(i))) {
                                        syntaxErrorList.addError(ErrorType.ParamClass, unaryExp.getIdent().getLineNum());
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                travelSyntaxTree(syntaxNode);
            }
        }
    }
}
