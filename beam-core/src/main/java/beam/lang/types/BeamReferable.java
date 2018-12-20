package beam.lang.types;

public abstract class BeamReferable {

    private BeamBlock parentBlock;
    private int line;
    private int column;
    private String path;

    public BeamBlock getParentBlock() {
        return parentBlock;
    }

    public abstract boolean resolve();

    public void setParentBlock(BeamBlock parentBlock) {
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
