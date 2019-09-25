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

import java.util.Optional;

import org.antlr.v4.runtime.IntStream;
import org.apache.commons.lang3.StringUtils;

public interface Locatable {

    GyroCharStream getStream();

    int getStartLine();

    int getStartColumn();

    int getStopLine();

    int getStopColumn();

    default String getFile() {
        return Optional.ofNullable(getStream())
            .map(GyroCharStream::getSourceName)
            .orElse(IntStream.UNKNOWN_SOURCE_NAME);
    }

    default String toLocation() {
        if (getStream() == null) {
            return null;
        }

        int startLine = getStartLine();
        int startColumn = getStartColumn();
        int stopLine = getStopLine();
        int stopColumn = getStopColumn();

        if (startLine == stopLine) {
            if (startColumn == stopColumn) {
                return String.format(
                    "on line @|bold %s|@ at column @|bold %s|@",
                    startLine + 1,
                    startColumn + 1);

            } else {
                return String.format(
                    "on line @|bold %s|@ from column @|bold %s|@ to @|bold %s|@",
                    startLine + 1,
                    startColumn + 1,
                    stopColumn + 1);
            }

        } else {
            return String.format(
                "from line @|bold %s|@ at column @|bold %s|@ to line @|bold %s|@ at column @|bold %s|@",
                startLine + 1,
                startColumn + 1,
                stopLine + 1,
                stopColumn + 1);
        }
    }

    default String toCodeSnippet() {
        GyroCharStream stream = getStream();

        if (stream == null) {
            return null;
        }

        int startLine = getStartLine();
        int startColumn = getStartColumn();
        int stopLine = getStopLine();
        int stopColumn = getStopColumn();

        StringBuilder text = new StringBuilder();
        int previousLine = startLine - 1;
        String previous = stream.getLineText(previousLine);
        String format = "%" + String.valueOf(stopLine + 1).length() + "d: ";

        if (!StringUtils.isBlank(previous)) {
            text.append(String.format(format, previousLine + 1));
            text.append(previous);
            text.append('\n');
        }

        for (int line = startLine; line <= stopLine; line ++) {
            String current = stream.getLineText(line);
            int end = current.length();
            int start = line == startLine ? startColumn : 0;
            int stop = line == stopLine ? stopColumn + 1 : end;

            text.append(String.format(format, line + 1));

            if (start >= 1) {
                text.append(current, 0, start);
            }

            text.append("@|red,underline ");
            text.append(current, start, stop > end ? end : stop);
            text.append("|@");

            if (end > stop) {
                text.append(current, stop, end);
            }

            text.append('\n');
        }

        int nextLine = stopLine + 1;
        String next = stream.getLineText(nextLine);

        if (!StringUtils.isBlank(next)) {
            text.append(String.format(format, nextLine + 1));
            text.append(next);
            text.append('\n');
        }

        return text.toString();
    }

}
