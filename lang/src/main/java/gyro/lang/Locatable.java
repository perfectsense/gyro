package gyro.lang;

public interface Locatable {

    String getFile();

    int getStartLine();

    int getStartColumn();

    int getStopLine();

    int getStopColumn();

    default String toLocation() {
        int startLine = getStartLine();
        int startColumn = getStartColumn();
        int stopLine = getStopLine();
        int stopColumn = getStopColumn();

        if (startLine == stopLine) {
            if (startColumn == stopColumn) {
                return String.format(
                    "on line @|bold %s|@ at column @|bold %s|@",
                    startLine + 1,
                    startColumn + 1);

            } else {
                return String.format(
                    "on line @|bold %s|@ from column @|bold %s|@ to @|bold %s|@",
                    startLine + 1,
                    startColumn + 1,
                    stopColumn + 1);
            }

        } else {
            return String.format(
                "from line @|bold %s|@ at column @|bold %s|@ to line @|bold %s|@ at column @|bold %s|@",
                startLine + 1,
                startColumn + 1,
                stopLine + 1,
                stopColumn + 1);
        }
    }

    String toCodeSnippet();

}
