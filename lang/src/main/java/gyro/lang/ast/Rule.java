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

import gyro.lang.GyroCharStream;
import gyro.lang.Locatable;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

public abstract class Rule implements Locatable {

    protected final Token start;
    protected final Token stop;

    public Rule(Token start, Token stop) {
        this.start = start;
        this.stop = stop;
    }

    public Rule(ParserRuleContext context) {
        if (context != null) {
            start = context.getStart();
            stop = context.getStop();

        } else {
            start = null;
            stop = null;
        }
    }

    @Override
    public GyroCharStream getStream() {
        return start != null ? (GyroCharStream) start.getTokenSource().getInputStream() : null;
    }

    @Override
    public int getStartLine() {
        return start != null ? start.getLine() - 1 : -1;
    }

    @Override
    public int getStartColumn() {
        return start != null ? start.getCharPositionInLine() : -1;
    }

    @Override
    public int getStopLine() {
        return stop != null ? stop.getLine() - 1 : -1;
    }

    @Override
    public int getStopColumn() {
        if (stop == null) {
            return -1;
        }

        int column = stop.getCharPositionInLine();
        int startIndex = stop.getStartIndex();
        int stopIndex = stop.getStopIndex();

        if (startIndex >= 0 && stopIndex >= 0 && stopIndex > startIndex) {
            column += stopIndex - startIndex;
        }

        return column;
    }

}
