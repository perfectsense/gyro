package gyro.lang;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringUtils;

public class SyntaxError {

    private final GyroCharStream stream;
    private final String message;
    private final int line;
    private final int startColumn;
    private final int stopColumn;

    public SyntaxError(GyroCharStream stream, String message, int line, int column) {
        this.stream = stream;
        this.message = message;
        this.line = line;
        this.startColumn = column;
        this.stopColumn = column;
    }

    public SyntaxError(GyroCharStream stream, String message, Token token) {
        this.stream = stream;
        this.message = message;
        this.line = token.getLine() - 1;
        this.startColumn = token.getCharPositionInLine();

        int start = token.getStartIndex();
        int stop = token.getStopIndex();

        if (start >= 0 && stop >= 0 && stop > start) {
            this.stopColumn = this.startColumn + stop - start;

        } else {
            this.stopColumn = this.startColumn;
        }
    }

    public static String toLineMessage(int startLine, int startColumn, int stopLine, int stopColumn) {
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

    public static String toCodeSnippet(GyroCharStream stream, int startLine, int startColumn, int stopLine, int stopColumn) {
        StringBuilder text = new StringBuilder();
        int previousLine = startLine - 1;
        String previous = stream.getLineText(previousLine);
        String format = "%" + String.valueOf(stopLine + 1).length() + "d: ";

        if (!StringUtils.isBlank(previous)) {
            text.append(String.format(format, previousLine + 1));
            text.append(previous);
            text.append('\n');
        }

        for (int line = startLine; line <= stopLine; line ++) {
            String current = stream.getLineText(line);
            int end = current.length();
            int start = line == startLine ? startColumn : 0;
            int stop = line == stopLine ? stopColumn + 1 : end;

            text.append(String.format(format, line + 1));

            if (start > 1) {
                text.append(current, 0, start);
            }

            text.append("@|red,underline ");
            text.append(current, start, stop);
            text.append("|@");

            if (end > stop) {
                text.append(current, stop, end);
            }

            text.append('\n');
        }

        int nextLine = stopLine + 1;
        String next = stream.getLineText(nextLine);

        if (!StringUtils.isBlank(next)) {
            text.append(String.format(format, nextLine + 1));
            text.append(next);
            text.append('\n');
        }

        return text.toString();
    }

    public String getMessage() {
        return message;
    }

    public String toLineMessage() {
        return toLineMessage(line, startColumn, line, stopColumn);
    }

    public String toCodeSnippet() {
        return toCodeSnippet(stream, line, startColumn, line, stopColumn);
    }

}
