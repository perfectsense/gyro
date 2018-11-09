package beam.lang;

import com.google.common.collect.ImmutableSet;
import org.fusesource.jansi.AnsiRenderer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CLIBeamUI implements BeamUI {

    private static final Pattern NEWLINES = Pattern.compile("([\r\n]+)");

    private int indentLevel;
    private int indentSize = 4;

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
            throw new RuntimeException(error);
        }
    }

    private String readOption(Set<String> values) {
        System.out.flush();

        while (true) {
            String selected = readLine();

            if (values.contains(selected)) {
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
            System.out.print(" (Y/n) ");
            return !"n".equalsIgnoreCase(readOption(ImmutableSet.of("y", "Y", "n", "N", "")));

        } else if (Boolean.FALSE.equals(defaultValue)) {
            System.out.print(" (y/N) ");
            return "y".equalsIgnoreCase(readOption(ImmutableSet.of("y", "Y", "n", "N", "")));

        } else {
            System.out.print(" (y/n) ");
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
        return readLine();
    }

    @Override
    public void indent() {
        ++ indentLevel;
    }

    @Override
    public void unindent() {
        -- indentLevel;
    }

    private void writeIndentation() {
        System.out.print(getIndentation());
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

    private String ansi(String text) {
        return AnsiRenderer.test(text) ? AnsiRenderer.render(text) : text;
    }

    @Override
    public void write(String message, Object... arguments) {
        System.out.print(dump(message, arguments));
        System.out.flush();
    }

    @Override
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

        return ansi(sb.toString());
    }

    @Override
    public void writeError(Throwable error, String message, Object... arguments) {
        write(message, arguments);
        System.out.write('\n');

        if (error != null) {
            write("%s: ", error.getClass().getName());
            StringWriter sw = new StringWriter();
            error.printStackTrace(new PrintWriter(sw));
            write(sw.toString());
        }

        System.out.flush();
    }
}
