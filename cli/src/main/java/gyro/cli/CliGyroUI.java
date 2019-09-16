package gyro.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gyro.core.AuditableGyroUI;
import gyro.core.GyroException;
import com.google.common.collect.ImmutableSet;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiRenderer;

public class CliGyroUI extends AuditableGyroUI {

    private static final Pattern NEWLINES = Pattern.compile("([\r\n]+)");

    private boolean verbose;
    private int indentSize = 4;
    private int indentLevel;
    private boolean pendingIndentation = true;

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public int getIndentSize() {
        return indentSize;
    }

    public void setIndentSize(int indentSize) {
        this.indentSize = indentSize;
    }

    private String readLine() {
        try {
            return new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine().trim();

        } catch (IOException error) {
            throw new GyroException(error);
        }
    }

    private String readOption(Set<String> values) {
        System.out.flush();

        while (true) {
            String selected = readLine();

            if (values.contains(selected)) {
                sendAudit(selected);
                return selected;

            } else {
                write("[%s] isn't valid! Try again.\n", selected);
            }
        }
    }

    @Override
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

    @Override
    public void readEnter(String message, Object... arguments) {
        write(message, arguments);
        readLine();
    }

    @Override
    public <E extends Enum<E>> E readNamedOption(E options) {
        Class<E> optionsClass = options.getDeclaringClass();
        Set<String> values = new HashSet<>();

        for (E option : optionsClass.getEnumConstants()) {
            values.add(option.name());
            write("%s) %s", option.name(), option);
        }

        return Enum.valueOf(optionsClass, readOption(values));
    }

    @Override
    public String readPassword(String message, Object... arguments) {
        write(message, arguments);
        return new String(System.console().readPassword());
    }

    @Override
    public String readText(String message, Object... arguments) {
        write(message, arguments);
        String line = readLine();
        sendAudit(line);
        return line;
    }

    @Override
    public void indent() {
        ++ indentLevel;
    }

    @Override
    public void unindent() {
        -- indentLevel;
    }

    private void writeIndentation(StringBuilder sb) {
        if (pendingIndentation) {
            for (int i = 0, l = indentLevel * getIndentSize(); i < l; ++ i) {
                sb.append(' ');
            }

            pendingIndentation = false;
        }
    }

    @Override
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

        System.out.print(sb.toString());
        System.out.flush();

        sendAudit(sb.toString());
    }

    @Override
    public void replace(String message, Object... arguments) {
        System.out.print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).cursorToColumn(1));
        write(message, arguments);
    }

}
