package beam.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class CapturingInputStream extends InputStream {

    private final InputStream in;
    private final OutputStream capture;
    private static final String beginMarker = "begin--d3cc1f98-122c-4151-8f96-1f32e43f0ed9";
    private static final String endMarker = "end--d3cc1f98-122c-4151-8f96-1f32e43f0ed9";

    public CapturingInputStream(InputStream in, OutputStream capture) {
        this.in = in;
        this.capture = capture;
    }

    public static String getBeginMarker() {
        return beginMarker;
    }

    public static String getEndMarker() {
        return endMarker;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int read = super.read(b);
        writeBeginMarker();
        capture.write(b, 0, read);
        writeEndMarker();
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        writeBeginMarker();
        capture.write(b, off, read);
        writeEndMarker();
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void close() throws IOException {
        in.close();
    }

    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        capture.write(b);
        return b;
    }

    private void writeBeginMarker() throws IOException {
       capture.write(beginMarker.getBytes(Charset.defaultCharset()));
    }

    private void writeEndMarker() throws IOException {
        capture.write(endMarker.getBytes(Charset.defaultCharset()));
    }
}
