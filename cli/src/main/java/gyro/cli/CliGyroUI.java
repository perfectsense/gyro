package gyro.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gyro.core.GyroCore;
import gyro.core.GyroUI;
import com.google.common.collect.ImmutableSet;
import org.fusesource.jansi.AnsiRenderer;

public class CliGyroUI implements GyroUI {

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
        if (pendingIndentation) {
            for (int i = 0, l = indentLevel * getIndentSize(); i < l; ++ i) {
                System.out.print(' ');
            }

            pendingIndentation = false;
        }
    }

    @Override
    public void write(String message, Object... arguments) {
        String text = arguments != null && arguments.length > 0
                ? String.format(message, arguments)
                : message;

        if (AnsiRenderer.test(text)) {
            text = AnsiRenderer.render(text);
        }

        int offset = 0;

        for (Matcher m = NEWLINES.matcher(text); m.find();) {
            writeIndentation();
            System.out.print(text.substring(offset, m.start()));
            System.out.print(m.group(1));

            pendingIndentation = true;
            offset = m.end();
        }

        int length = text.length();

        if (length > offset) {
            writeIndentation();
            System.out.print(text.substring(offset, length));
        }

        System.out.flush();
    }

    @Override
    public void writeError(Throwable error, String message, Object... arguments) {
        write(message, arguments);
        if (error != null) {
            try {
                Path errorDir = Paths.get(GyroCore.getGyroUserHome(), ".gyro", "error");
                Files.createDirectories(errorDir);

                ZonedDateTime time = ZonedDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss z");
                Path log = Paths.get(errorDir.toString(), String.format("%s.log", formatter.format(time)));
                try (PrintWriter printWriter = new PrintWriter(Files.newBufferedWriter(log, StandardCharsets.UTF_8))) {
                    printWriter.write(String.format("%s: ", error.getClass().getName()));
                    error.printStackTrace(printWriter);
                    write("@|red See '%s' for more details.\n |@", log.toString());
                }
            } catch (IOException ioe) {
                System.out.write('\n');
                write("%s: ", error.getClass().getName());
                StringWriter sw = new StringWriter();
                error.printStackTrace(new PrintWriter(sw));
                write(sw.toString());
            }
        }

        System.out.write('\n');
        System.out.flush();
    }
}