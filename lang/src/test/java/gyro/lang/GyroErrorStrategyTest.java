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

import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GyroErrorStrategyTest {

    GyroErrorStrategy strategy;
    Parser recognizer;
    Token token;
    IntervalSet set;

    @BeforeEach
    void beforeEach() {
        strategy = spy(GyroErrorStrategy.INSTANCE);
        recognizer = mock(Parser.class);
        token = mock(Token.class);
        set = mock(IntervalSet.class);
    }

    @Test
    void reportInputMismatch() {
        InputMismatchException error = mock(InputMismatchException.class);

        when(error.getOffendingToken()).thenReturn(token);
        when(error.getExpectedTokens()).thenReturn(set);
        when(set.toString(any(Vocabulary.class))).thenReturn("foo");
        when(recognizer.getVocabulary()).thenReturn(mock(Vocabulary.class));

        strategy.reportInputMismatch(recognizer, error);

        verify(recognizer).notifyErrorListeners(token, "Expected foo", error);
    }

    @Test
    void reportMissingToken() {
        when(recognizer.getCurrentToken()).thenReturn(token);
        when(recognizer.getExpectedTokens()).thenReturn(set);
        when(set.toString(any(Vocabulary.class))).thenReturn("foo");
        when(recognizer.getVocabulary()).thenReturn(mock(Vocabulary.class));

        strategy.reportMissingToken(recognizer);

        assertThat(strategy.inErrorRecoveryMode(recognizer)).isTrue();
        verify(recognizer).notifyErrorListeners(token, "Missing foo", null);
    }

    @Test
    void reportMissingTokenInRecovery() {
        doReturn(true).when(strategy).inErrorRecoveryMode(recognizer);

        strategy.reportMissingToken(recognizer);

        verifyNoMoreInteractions(recognizer);
    }

    @Test
    void reportNoViableAlternative() {
        NoViableAltException error = mock(NoViableAltException.class);

        when(error.getOffendingToken()).thenReturn(token);

        strategy.reportNoViableAlternative(recognizer, error);

        verify(recognizer).notifyErrorListeners(token, "Invalid input", error);
    }

    @Test
    void reportUnwantedToken() {
        when(recognizer.getCurrentToken()).thenReturn(token);

        strategy.reportUnwantedToken(recognizer);

        assertThat(strategy.inErrorRecoveryMode(recognizer)).isTrue();
        verify(recognizer).notifyErrorListeners(token, "Extra input", null);
    }

    @Test
    void reportUnwantedTokenInRecovery() {
        doReturn(true).when(strategy).inErrorRecoveryMode(recognizer);

        strategy.reportUnwantedToken(recognizer);

        verifyNoMoreInteractions(recognizer);
    }

}