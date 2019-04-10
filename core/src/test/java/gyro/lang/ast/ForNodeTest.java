package gyro.lang.ast;

import gyro.lang.ast.control.ForNode;
import gyro.lang.ast.scope.Scope;
import gyro.lang.ast.value.LiteralStringNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ForNodeTest {

    @Test
    void evaluateEmptyBody() throws Exception {
        Node body = mock(Node.class);

        ForNode node = new ForNode(
                Arrays.asList("foo", "bar"),
                Arrays.asList(
                        new LiteralStringNode("foo1"),
                        new LiteralStringNode("bar2"),
                        new LiteralStringNode("foo3"),
                        new LiteralStringNode("bar2")),
                Collections.singletonList(body));

        Scope scope = new Scope(null);
        ArgumentCaptor<Scope> args = ArgumentCaptor.forClass(Scope.class);

        node.evaluate(scope);
        verify(body, times(2)).evaluate(args.capture());
        args.getAllValues().forEach(s -> assertThat(s.getParent(), is(scope)));
        assertThat(scope.size(), is(0));
    }
}