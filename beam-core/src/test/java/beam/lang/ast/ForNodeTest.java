package beam.lang.ast;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForNodeTest {

    @Test
    void evaluateEmptyBody() {
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