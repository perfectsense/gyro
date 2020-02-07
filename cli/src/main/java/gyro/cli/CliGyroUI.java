/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.auditor.GyroAuditor;
import gyro.core.command.GyroCommand;
import org.fusesource.jansi.Ansi;
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
            throw new GyroException(error);
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
        ++indentLevel;
    }

    @Override
    public void unindent() {
        --indentLevel;
    }

    private void writeIndentation(StringBuilder outputBuilder) {
        if (pendingIndentation) {
            if (indentLevel > 0) {
                outputBuilder.append(String.format("%" + indentLevel * getIndentSize() + "s", ""));
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
        StringBuilder outputBuilder = new StringBuilder();

        for (Matcher m = NEWLINES.matcher(text); m.find(); ) {
            writeIndentation(outputBuilder);
            outputBuilder.append(text, offset, m.start());
            outputBuilder.append(m.group(1));

            pendingIndentation = true;
            offset = m.end();
        }

        int length = text.length();

        if (length > offset) {
            writeIndentation(outputBuilder);
            outputBuilder.append(text, offset, length);
        }
        String output = outputBuilder.toString();
        System.out.print(output);
        System.out.flush();

        GyroCommand command = GyroCore.getCommand();

        if (command != null && command.enableAuditor()) {
            GyroAuditor.AUDITOR_BY_NAME.values().stream()
                .parallel()
                .filter(GyroAuditor::isStarted)
                .filter(auditor -> !auditor.isFinished())
                .forEach(auditor -> {
                    try {
                        auditor.append(output);
                    } catch (Exception ex) {
                        // TODO: message
                        System.err.print(ex.getMessage());
                    }
                });
        }
    }

    @Override
    public void replace(String message, Object... arguments) {
        System.out.print(Ansi.ansi().eraseLine(Ansi.Erase.ALL).cursorToColumn(1));
        write(message, arguments);
    }

}
