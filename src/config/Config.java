package config;
import tools.IOFunc;

import java.io.IOException;

public class Config {
    final public static String fileInPutPath = "testfile.txt";
    final public static String fileOutPutPath = "output.txt";
    final public static  String ERROR_FILE = "error.txt";
    final public static String irOutPutPath = "llvm_ir.txt";
    final public static String mipsOutPutPath = "mips.txt";
    final public static boolean rawMipsOutPutPath = true;
    public static final boolean irError = true;// 是否检查 ir 过程中的程序错误
    public static void init() throws IOException {
        IOFunc.clear(fileOutPutPath);
        IOFunc.clear(ERROR_FILE);
        IOFunc.clear(irOutPutPath);
        IOFunc.clear(mipsOutPutPath);
    }
}
