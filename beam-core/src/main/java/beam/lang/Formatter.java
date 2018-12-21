package beam.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Formatter {

    private static final Pattern NEWLINES = Pattern.compile("([\r\n]+)");

    private int indentLevel;
    private int indentSize = 4;

    public int getIndentSize() {
        return indentSize;
    }

    public void setIndentSize(int indentSize) {
        this.indentSize = indentSize;
    }

    public void indent() {
        ++ indentLevel;
    }

    public void unindent() {
        -- indentLevel;
    }

    private String getIndentation() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; ++ i) {
            for (int j = getIndentSize(); j > 0; -- j) {
                sb.append(' ');
            }
        }

        return sb.toString();
    }

    public String dump(String message, Object... arguments) {
        String text = arguments != null && arguments.length > 0
                ? String.format(message, arguments)
                : message;

        int offset = 0;

        StringBuilder sb = new StringBuilder();
        for (Matcher m = NEWLINES.matcher(text); m.find();) {
            sb.append(getIndentation());
            sb.append(text.substring(offset, m.start()));
            sb.append(m.group(1));

            offset = m.end();
        }


        int length = text.length();

        if (length > offset) {
            sb.append(getIndentation());
            sb.append(text.substring(offset, length));
        }

        return sb.toString();
    }
}
