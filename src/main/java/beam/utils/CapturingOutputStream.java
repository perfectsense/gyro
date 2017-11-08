package beam.utils;

import java.io.IOException;
import java.io.OutputStream;

public class CapturingOutputStream extends OutputStream {

    private final OutputStream out;
    private final OutputStream capture;

    public CapturingOutputStream(OutputStream out, OutputStream capture) {
        this.out = out;
        this.capture = capture;
    }

    @Override
    public void write(byte[] b) throws IOException {
        out.write(b);
        capture.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        capture.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        capture.write(b);
    }
}
