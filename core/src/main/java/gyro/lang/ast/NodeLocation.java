package gyro.lang.ast;

public class NodeLocation {

    private final String file;
    private final Integer line;
    private final Integer column;

    public NodeLocation(String file, Integer line, Integer column) {
        this.file = file;
        this.line = line;
        this.column = column;
    }

    public String getFile() {
        return file;
    }

    public Integer getLine() {
        return line;
    }

    public Integer getColumn() {
        return column;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (file != null) {
            sb.append("in ");
            sb.append(file);
            sb.append(" ");
        }

        if (line != null) {
            sb.append("on line ");
            sb.append(line);
            sb.append(" ");
        }

        if (column != null) {
            sb.append("at column ");
            sb.append(column);
            sb.append(" ");
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
            sb.append(": ");
        }

        return sb.toString();
    }
}
