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