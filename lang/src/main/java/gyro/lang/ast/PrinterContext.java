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

package gyro.lang.ast;

import java.io.IOException;

public class PrinterContext implements Appendable {

    private final Appendable appendable;
    private final int indentDepth;

    public PrinterContext(Appendable appendable, int indentDepth) {
        this.appendable = appendable;
        this.indentDepth = indentDepth;
    }

    public PrinterContext indented() {
        return new PrinterContext(appendable, indentDepth + 1);
    }

    public void appendNewline() throws IOException {
        appendable.append('\n');

        for (int i = 0; i < indentDepth; i++) {
            appendable.append("    ");
        }
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        return appendable.append(csq);
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
        return appendable.append(csq, start, end);
    }

    @Override
    public Appendable append(char c) throws IOException {
        return appendable.append(c);
    }

}
