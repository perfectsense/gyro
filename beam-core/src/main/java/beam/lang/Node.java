package beam.lang;

import beam.core.BeamCore;
import org.apache.commons.lang.StringUtils;

public abstract class Node {

    private transient Node parent;
    private transient int line;
    private transient int column;
    private BeamCore core;
    private BeamFile fileNode;

    public abstract boolean resolve();

    public abstract String serialize(int indent);

    public Node parent() {
        return parent;
    }

    public void parent(Node parent) {
        this.parent = parent;
    }

    public int line() {
        return line;
    }

    public void line(int line) {
        this.line = line;
    }

    public int column() {
        return column;
    }

    public void column(int column) {
        this.column = column;
    }

    public BeamCore core() {
        return core;
    }

    public void core(BeamCore core) {
        this.core = core;
    }

    public BeamFile fileNode() {
        if (fileNode == null) {
            Node parent = parent();

            while (parent != null && !(parent instanceof BeamFile)) {
                parent = parent.parent();
            }

            if (parent instanceof BeamFile) {
                fileNode = (BeamFile) parent;
            }
        }

        return fileNode;
    }

    protected String indent(int indent) {
        return StringUtils.repeat(" ", indent);
    }

}
