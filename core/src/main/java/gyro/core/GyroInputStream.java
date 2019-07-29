package gyro.core;

import gyro.core.backend.FileBackend;

import java.io.IOException;
import java.io.InputStream;

public class GyroInputStream extends InputStream {

    private final InputStream input;
    private final String message;

    public GyroInputStream(FileBackend backend, String file) {
        try {
            this.input = backend.openInput(file);

        } catch (Exception error) {
            throw new GyroException(
                String.format("Can't open @|bold %s|@ in @|bold %s|@ for reading!", file, backend),
                error);
        }

        this.message = String.format("Can't read from @|bold %s|@ in @|bold %s|@!", file, backend);
    }

    @Override
    public int available() {
        try {
            return input.available();

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

    @Override
    public void close() {
        try {
            input.close();

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

    @Override
    public void mark(int limit) {
        input.mark(limit);
    }

    @Override
    public boolean markSupported() {
        return input.markSupported();
    }

    @Override
    public int read() {
        try {
            return input.read();

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

    @Override
    public int read(byte[] buffer) {
        try {
            return input.read(buffer);

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) {
        try {
            return input.read(buffer, offset, length);

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

    @Override
    public long skip(long n) {
        try {
            return input.skip(n);

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

    @Override
    public void reset() {
        try {
            input.reset();

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

}
