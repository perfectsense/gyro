package gyro.lang.ast;

import gyro.lang.ast.value.BooleanNode;
import gyro.parser.antlr4.GyroParser;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BooleanNodeTest {

    @Mock
    private GyroParser.BooleanValueContext context;

    @Test
    void evaluateTrue() {
        when(context.TRUE()).thenReturn(mock(TerminalNode.class));
        assertThat(new BooleanNode(context).evaluate(null), is(Boolean.TRUE));
    }

    @Test
    void evaluateFalse() {
        when(context.TRUE()).thenReturn(null);
        assertThat(new BooleanNode(context).evaluate(null), is(Boolean.FALSE));
    }
}