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
