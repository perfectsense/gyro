package gyro.lang;

import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringUtils;

public class SyntaxError {

    private final GyroCharStream stream;
    private final String message;
    private final int line;
    private final int column;
    private final Token token;

    public SyntaxError(GyroCharStream stream, String message, int line, int column, Token token) {
        this.stream = stream;
        this.message = message;
        this.line = line;
        this.column = column;
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String toCode() {
        StringBuilder text = new StringBuilder();
        String previous = stream.getLine(line - 1);
        String format = "%" + String.valueOf(line + 1).length() + "d: ";

        if (!StringUtils.isBlank(previous)) {
            text.append(String.format(format, line - 1));
            text.append(previous);
            text.append('\n');
        }

        String current = stream.getLine(line);
        int currentLength = current.length();
        int start;
        int stop;

        if (token != null) {
            start = token.getStartIndex();
            stop = token.getStopIndex();

        } else {
            start = -1;
            stop = -1;
        }

        text.append(String.format(format, line));

        if (start >= 0 && stop >= 0 && start < currentLength && stop > start) {
            if (start > 1) {
                text.append(current, 0, start);
            }

            text.append("@|red,underline ");
            text.append(current, start, stop - start);
            text.append("|@");
            text.append(current, stop, currentLength - stop);

        } else {
            if (column > 1) {
                text.append(current, 0, column);
            }

            text.append("@|red,underline ");
            text.append(current.substring(column));
            text.append("|@");
        }

        text.append('\n');

        String next = stream.getLine(line + 1);

        if (!StringUtils.isBlank(next)) {
            text.append(String.format(format, line + 1));
            text.append(next);
            text.append('\n');
        }

        return text.toString();
    }

}
