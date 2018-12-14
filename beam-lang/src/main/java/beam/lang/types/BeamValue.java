package beam.lang.types;

import beam.lang.BeamReferable;

public abstract class BeamValue implements BeamReferable {

    private Integer line;

    private String path;

    public Integer getLine() {
        return line;
    }

    public void setLine(Integer line) {
        this.line = line;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
