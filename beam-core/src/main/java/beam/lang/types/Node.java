package beam.lang.types;

public abstract class Node {

    private transient Node parentBlock;
    private transient int line;
    private transient int column;
    private transient String path;

    public Node getParentBlock() {
        return parentBlock;
    }

    public abstract boolean resolve();

    public void setParentBlock(Node parentBlock) {
        this.parentBlock = parentBlock;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getColumn() {
        return column;
    }

    public void setColumn(int column) {
        this.column = column;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

}
