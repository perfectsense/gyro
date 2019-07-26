package gyro.lang;

public class TestLocatable implements Locatable {

    private final GyroCharStream stream;
    private final int startLine;
    private final int startColumn;
    private final int stopLine;
    private final int stopColumn;

    public TestLocatable(GyroCharStream stream, int startLine, int startColumn, int stopLine, int stopColumn) {
        this.stream = stream;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.stopLine = stopLine;
        this.stopColumn = stopColumn;
    }

    @Override
    public GyroCharStream getStream() {
        return stream;
    }

    @Override
    public int getStartLine() {
        return startLine;
    }

    @Override
    public int getStartColumn() {
        return startColumn;
    }

    @Override
    public int getStopLine() {
        return stopLine;
    }

    @Override
    public int getStopColumn() {
        return stopColumn;
    }

}
