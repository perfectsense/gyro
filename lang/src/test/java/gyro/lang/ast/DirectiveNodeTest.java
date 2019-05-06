package gyro.lang.ast;

import java.util.Collections;
import java.util.List;

import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectiveNodeTest extends AbstractNodeTest<DirectiveNode> {

    @Test
    void constructorContext() {
        DirectiveNode node = new DirectiveNode(parse("@foo 'bar'", GyroParser::directive));
        List<ValueNode> arguments = node.getArguments();

        assertThat(node.getName()).isEqualTo("foo");
        assertThat(arguments).hasSize(1);
        assertThat(arguments.get(0).getValue()).isEqualTo("bar");
    }

    @Test
    void getName() {
        String name = "foo";
        DirectiveNode node = new DirectiveNode(name, Collections.emptyList());

        assertThat(node.getName()).isEqualTo(name);
    }

    @Test
    void getArguments() {
        ValueNode argument0 = mock(ValueNode.class);
        DirectiveNode node = new DirectiveNode("foo", Collections.singletonList(argument0));

        assertThat(node.getArguments()).containsExactly(argument0);
    }

    @Test
    void getArgumentsImmutable() {
        DirectiveNode node = new DirectiveNode("foo", Collections.emptyList());

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> node.getArguments().add(mock(ValueNode.class)));
    }

}