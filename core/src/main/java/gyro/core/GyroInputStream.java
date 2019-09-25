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

package gyro.core;

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
