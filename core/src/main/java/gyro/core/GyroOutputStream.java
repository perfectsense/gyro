package gyro.core;

import gyro.core.backend.FileBackend;

import java.io.IOException;
import java.io.OutputStream;

public class GyroOutputStream extends OutputStream {

    private final OutputStream output;
    private final String message;

    public GyroOutputStream(FileBackend backend, String file) {
        try {
            this.output = backend.openOutput(file);

        } catch (Exception error) {
            throw new GyroException(
                String.format("Can't open @|bold %s|@ in @|bold %s|@ for writing!", file, backend),
                error);
        }

        this.message = String.format("Can't write to @|bold %s|@ in @|bold %s|@!", file, backend);
    }

    @Override
    public void close() {
        try {
            output.close();

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

    @Override
    public void flush() {
        try {
            output.flush();

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

    @Override
    public void write(int b) {
        try {
            output.write(b);

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

    @Override
    public void write(byte[] b) {
        try {
            output.write(b);

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

    @Override
    public void write(byte[] buffer, int offset, int length) {
        try {
            output.write(buffer, offset, length);

        } catch (IOException error) {
            throw new GyroException(message, error);
        }
    }

}
