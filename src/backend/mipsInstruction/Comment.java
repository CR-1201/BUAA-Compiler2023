package backend.mipsInstruction;
/**
 * 包括两个东西,一个是宏调用,一个是注释
 */
public class Comment extends Instruction{
    private final String content;
    private final boolean isComment;

    public Comment(String content) {
        this.content = content;
        this.isComment = true;
    }

    public Comment(String content, boolean isComment) {
        this.content = content;
        this.isComment = isComment;
    }

    @Override
    public String toString() {
        if (isComment) {
            return "# " + content + "\n";
        } else {
            return content + "\n";
        }
    }
}
