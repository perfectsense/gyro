package beam.lang.ast;

import beam.lang.ast.control.ForNode;
import beam.lang.ast.scope.Scope;
import beam.lang.ast.value.StringNode;
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
                        new StringNode("foo1"),
                        new StringNode("bar2"),
                        new StringNode("foo3"),
                        new StringNode("bar2")),
                Collections.singletonList(body));

        Scope scope = new Scope(null);
        ArgumentCaptor<Scope> args = ArgumentCaptor.forClass(Scope.class);

        node.evaluate(scope);
        verify(body, times(2)).evaluate(args.capture());
        args.getAllValues().forEach(s -> assertThat(s.getParent(), is(scope)));
        assertThat(scope.size(), is(0));
    }
}