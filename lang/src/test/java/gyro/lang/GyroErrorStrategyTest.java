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