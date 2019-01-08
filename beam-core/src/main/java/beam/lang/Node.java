package beam.lang;

public abstract class Node {

    private transient Node parentNode;
    private transient int line;
    private transient int column;

    public abstract boolean resolve();

    public Node parentNode() {
        return parentNode;
    }

    public void setParentNode(Node parentNode) {
        this.parentNode = parentNode;
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

}
