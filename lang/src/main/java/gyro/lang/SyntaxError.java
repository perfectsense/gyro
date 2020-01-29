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

package gyro.lang;

import org.antlr.v4.runtime.Token;

public class SyntaxError implements Locatable {

    private final GyroCharStream stream;
    private final String message;
    private final int line;
    private final int startColumn;
    private final int stopColumn;

    public SyntaxError(GyroCharStream stream, String message, int line, int column) {
        this.stream = stream;
        this.message = message;
        this.line = line;
        this.startColumn = column;
        this.stopColumn = column;
    }

    public SyntaxError(GyroCharStream stream, String message, Token token) {
        this.stream = stream;
        this.message = message;
        this.line = token.getLine() - 1;
        this.startColumn = token.getCharPositionInLine();

        int start = token.getStartIndex();
        int stop = token.getStopIndex();

        if (start >= 0 && stop >= 0 && stop > start) {
            this.stopColumn = this.startColumn + stop - start;

        } else {
            this.stopColumn = this.startColumn;
        }
    }

    public String getMessage() {
        return message;
    }

    @Override
    public GyroCharStream getStream() {
        return stream;
    }

    @Override
    public int getStartLine() {
        return line;
    }

    @Override
    public int getStartColumn() {
        return startColumn;
    }

    @Override
    public int getStopLine() {
        return line;
    }

    @Override
    public int getStopColumn() {
        return stopColumn;
    }

}
