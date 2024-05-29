import Pass.passControl;
import backend.CodeGen;
import backend.mipsComponent.Module;
import backend.optimizer.RegOptimizer;
import config.Config;
import error.ErrorHandler;
import frontend.LexicalAnalyze;
import frontend.ParserAnalyze;
import ir.irBuilder;
import ir.module;
import node.CompUnit;
import tools.IOFunc;

import java.io.IOException;
import java.util.Objects;

import static backend.CodeGen.getCodeGen;

public class Compiler {
    public static void main(String[] args) throws IOException {
        Config.init();

        LexicalAnalyze.getLexical().analyze(IOFunc.input(Config.fileInPutPath));
        LexicalAnalyze.getLexical().printTokens();

//        if(LexicalAnalyze.getLexical().checkTokens())return;

        ParserAnalyze.getParser().setTokens(LexicalAnalyze.getLexical().getTokens());
        ParserAnalyze.getParser().analyze();
        ParserAnalyze.getParser().printParseAns();

        CompUnit syntaxTreeRoot = ParserAnalyze.getParser().getCompUnit();
        ErrorHandler errorHandler = new ErrorHandler(syntaxTreeRoot,ParserAnalyze.getParser().getErrorList());
        String error = errorHandler.getErrorList();
        if(!Objects.equals(error, "")){ // 有错误就终止 llvm ir 和 mips 的生成
            IOFunc.output(error,Config.ERROR_FILE);
        }else{
            irBuilder.getIrBuilder().buildModule(syntaxTreeRoot);
            passControl pass= new passControl();
            pass.run();
            IOFunc.output(module.getModule().toString(),Config.irOutPutPath);

            // np-optimize backend code generation
            CodeGen codeGen = getCodeGen();
            Module mipsModule = codeGen.buildModule();
            String rawMips;
            if (Config.rawMipsOutPutPath) {
                rawMips = mipsModule.toString();
            } else {
                rawMips = null;
            }
            RegOptimizer regOptimizer = new RegOptimizer(mipsModule);
            regOptimizer.process();
            IOFunc.output(mipsModule.toString(),Config.mipsOutPutPath);
        }
    }
}