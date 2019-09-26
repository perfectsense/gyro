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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GyroErrorListenerTest extends AbstractSyntaxErrorTest {

    GyroErrorListener listener;

    @BeforeEach
    void beforeEach() {
        listener = new GyroErrorListener(stream);
    }

    void assertListener(int startLine, int startColumn, int stopLine, int stopColumn) {
        assertThat(listener.getSyntaxErrors()).hasSize(1);

        assertSyntaxError(
            listener.getSyntaxErrors().get(0),
            startLine,
            startColumn,
            stopLine,
            stopColumn);
    }

    @Test
    void syntaxErrorNull() {
        listener.syntaxError(null, null, 10, 20, MESSAGE, null);
        assertListener(9, 20, 9, 20);
    }

    @Test
    void syntaxErrorToken() {
        Token token = mock(Token.class);

        when(token.getLine()).thenReturn(10);
        when(token.getCharPositionInLine()).thenReturn(20);
        when(token.getStartIndex()).thenReturn(-1);
        when(token.getStopIndex()).thenReturn(-1);

        listener.syntaxError(null, token, 0, 0, MESSAGE, null);
        assertListener(9, 20, 9, 20);
    }

    @Test
    void syntaxErrorTokenStartStop() {
        Token token = mock(Token.class);

        when(token.getLine()).thenReturn(10);
        when(token.getCharPositionInLine()).thenReturn(20);
        when(token.getStartIndex()).thenReturn(30);
        when(token.getStopIndex()).thenReturn(40);

        listener.syntaxError(null, token, 0, 0, MESSAGE, null);
        assertListener(9, 20, 9, 30);
    }

}