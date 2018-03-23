package beam.core;

import java.util.HashMap;
import java.util.Map;

public class BeamConfigLocation {

    private int line;
    private int column;
    private Map<String, String> contentMap;

    public BeamConfigLocation(int line, int column) {
        this.line = line;
        this.column = column;
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

    public Map<String, String> getContentMap() {
        if (contentMap == null) {
            contentMap = new HashMap<>();
        }

        return contentMap;
    }

    public void setContentMap(Map<String, String> contentMap) {
        this.contentMap = contentMap;
    }
}
