package gyro.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import gyro.util.Bug;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.misc.Interval;

public class GyroCharStream implements CharStream {

    private final List<String> lines;
    private final CharStream stream;

    private List<String> readLines(Reader reader) throws IOException {
        try (BufferedReader buffered = new BufferedReader(reader)) {
            List<String> lines = new ArrayList<>();

            for (String line; (line = buffered.readLine()) != null; ) {
                lines.add(line);
            }

            return lines;
        }
    }

    private CharStream createStream(List<String> lines, String file) {
        return CharStreams.fromString(
            String.join("\n", lines),
            file != null ? file : IntStream.UNKNOWN_SOURCE_NAME);
    }

    public GyroCharStream(InputStream input, String file) throws IOException {
        lines = readLines(new InputStreamReader(input, StandardCharsets.UTF_8));
        stream = createStream(lines, file);
    }

    public GyroCharStream(String text) {
        try {
            lines = readLines(new StringReader(text));

        } catch (IOException error) {
            throw new Bug(error);
        }

        stream = createStream(lines, null);
    }

    public String getLine(int line) {
        int index = line - 1;
        return index >= 0 && index < lines.size() ? lines.get(index) : null;
    }

    @Override
    public void consume() {
        stream.consume();
    }

    @Override
    public String getSourceName() {
        return stream.getSourceName();
    }

    @Override
    public String getText(Interval interval) {
        return stream.getText(interval);
    }

    @Override
    public int index() {
        return stream.index();
    }

    @Override
    public int LA(int i) {
        return stream.LA(i);
    }

    @Override
    public int mark() {
        return stream.mark();
    }

    @Override
    public void release(int marker) {
        stream.release(marker);
    }

    @Override
    public void seek(int index) {
        stream.seek(index);
    }

    @Override
    public int size() {
        return stream.size();
    }

}
