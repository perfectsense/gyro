package beam.lang.types;

import java.util.HashSet;
import java.util.Set;

public abstract class BeamValue<T> extends BeamReferable {

    private int line;
    private int column;
    private Set<BeamBlock> dependencies;

    private String path;

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

    public Set<BeamBlock> dependencies() {
        if (dependencies == null) {
            dependencies = new HashSet<>();
        }

        return dependencies;
    }

    public abstract T getValue();

}
