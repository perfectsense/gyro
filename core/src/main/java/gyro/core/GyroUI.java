package gyro.core;

import com.google.common.collect.ImmutableSet;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiRenderer;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GyroUI {

    private static final Pattern NEWLINES = Pattern.compile("([\r\n]+)");

    private Reader reader;
    private Writer writer;
    private Console console;
    private boolean verbose;
    private int indentSize = 4;
    private int indentLevel;
    private boolean pendingIndentation = true;

    public GyroUI() {
        this.console = System.console();
        this.reader = console == null ? new InputStreamReader(System.in, StandardCharsets.UTF_8) : System.console().reader();
        this.writer = console == null ? new PrintWriter(System.out) : System.console().writer();
    }

    public GyroUI(Reader reader, Writer writer) {
        this.reader = reader;
        this.writer = writer;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public int getIndentSize() {
        return indentSize;
    }

    public void setIndentSize(int indentSize) {
        this.indentSize = indentSize;
    }

    public boolean readBoolean(Boolean defaultValue, String message, Object... arguments) {
        write(message, arguments);

        if (Boolean.TRUE.equals(defaultValue)) {
            write(" (Y/n) ");
            return !"n".equalsIgnoreCase(readOption(ImmutableSet.of("y", "Y", "n", "N", "")));

        } else if (Boolean.FALSE.equals(defaultValue)) {
            write(" (y/N) ");
            return "y".equalsIgnoreCase(readOption(ImmutableSet.of("y", "Y", "n", "N", "")));

        } else {
            write(" (y/n) ");
            return "y".equalsIgnoreCase(readOption(ImmutableSet.of("y", "Y", "n", "N")));
        }
    }

    public String readPassword(String message, Object... arguments) {
        write(message, arguments);

        if (console != null) {
            return new String(System.console().readPassword());
        }

        return readLine();
    }

    public String readText(String message, Object... arguments) {
        write(message, arguments);
        return readLine();
    }

    public void indent() {
        ++ indentLevel;
    }

    public void unindent() {
        -- indentLevel;
    }

    public <E extends Throwable> void indented(ThrowingProcedure<E> procedure) throws E {
        indent();

        try {
            procedure.execute();

        } finally {
            unindent();
        }
    }

    private void writeIndentation(StringBuilder sb) {
        if (pendingIndentation) {
            for (int i = 0, l = indentLevel * getIndentSize(); i < l; ++ i) {
                sb.append(' ');
            }

            pendingIndentation = false;
        }
    }

    public void write(String message, Object... arguments) {
        StringBuilder sb = new StringBuilder();

        String text = arguments != null && arguments.length > 0
            ? String.format(message, arguments)
            : message;

        if (AnsiRenderer.test(text)) {
            text = AnsiRenderer.render(text);
        }

        int offset = 0;

        for (Matcher m = NEWLINES.matcher(text); m.find();) {
            writeIndentation(sb);
            sb.append(text.substring(offset, m.start()));
            sb.append(m.group(1));

            pendingIndentation = true;
            offset = m.end();
        }

        int length = text.length();

        if (length > offset) {
            writeIndentation(sb);
            sb.append(text.substring(offset, length));
        }

        print(sb.toString());
    }

    public void replace(String message, Object... arguments) {
        print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).cursorToColumn(1).toString());
        write(message, arguments);
    }

    private void print(String s) {
        try {
            writer.write(s);
            writer.flush();
        } catch (IOException ioe) {
        }
    }

    private void println(String s) {
        print(s);
        print("\n");
    }

    private String readOption(Set<String> values) {
        while (true) {
            String selected = readLine();

            if (values.contains(selected)) {
                return selected;

            } else {
                write("[%s] isn't valid! Try again.\n", selected);
            }
        }
    }

    private String readLine() {
        try {
            if (console != null) {
                return console.readLine();
            }

            return new BufferedReader(reader).readLine().trim();
        } catch (IOException error) {
            throw new GyroException(error);
        }
    }

}
