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
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class SyntaxErrorTest extends AbstractSyntaxErrorTest {

    @Test
    void constructor() {
        assertSyntaxError(new SyntaxError(stream, MESSAGE, 10, 20), 10, 20, 10, 20);
    }

    @Test
    void constructorToken() {
        Token token = mock(Token.class);

        when(token.getLine()).thenReturn(10);
        when(token.getCharPositionInLine()).thenReturn(20);
        when(token.getStartIndex()).thenReturn(-1);
        when(token.getStopIndex()).thenReturn(-1);

        assertSyntaxError(new SyntaxError(stream, MESSAGE, token), 9, 20, 9, 20);
    }

    @Test
    void constructorTokenStartStop() {
        Token token = mock(Token.class);

        when(token.getLine()).thenReturn(10);
        when(token.getCharPositionInLine()).thenReturn(20);
        when(token.getStartIndex()).thenReturn(30);
        when(token.getStopIndex()).thenReturn(40);

        assertSyntaxError(new SyntaxError(stream, MESSAGE, token), 9, 20, 9, 30);
    }

}