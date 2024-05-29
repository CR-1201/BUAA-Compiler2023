package node;

import token.Token;

import java.io.PrintStream;
import java.util.List;

public class Block extends FatherNode{
    // Block -> '{' { BlockItem } '}'
    private final Token lbrace;
    private final List<BlockItem> blockItems;
    private final Token rbrace;
    public Block(Token lbrace, List<BlockItem> blockItems, Token rbrace) {
        this.lbrace = lbrace;
        this.blockItems = blockItems;
        this.rbrace = rbrace;
        childrenNode.addAll(blockItems);
    }

    public void output(PrintStream ps){
        ps.print(lbrace.toString());
        for (BlockItem blockItem : blockItems) {
            blockItem.output(ps);
        }
        ps.print(rbrace.toString());
        ps.println("<Block>");
    }

    public List<BlockItem> getBlockItems() {
        return blockItems;
    }

    public Token getRbrace() {
        return rbrace;
    }
    @Override
    public void buildIrTree(){
        FatherNode.irSymbolTable.pushBlockLayer();
        for (FatherNode blockItem : blockItems) {
            blockItem.buildIrTree();
        }
        FatherNode.irSymbolTable.popBlockLayer();
    }
}
