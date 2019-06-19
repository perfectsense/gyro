package gyro.lang.ast;

import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PairNodeTest extends AbstractNodeTest<PairNode> {

    @Test
    void constructorContext() {
        PairNode node = (PairNode) Node.parse("foo: 'bar'", GyroParser::pair);
        Node valueNode = node.getValue();

        assertThat(node.getKey()).isEqualTo("foo");
        assertThat(valueNode).isInstanceOf(ValueNode.class);
        assertThat(((ValueNode) valueNode).getValue()).isEqualTo("bar");
    }

    @Test
    void getKey() {
        String key = "foo";
        PairNode node = new PairNode(key, mock(Node.class));

        assertThat(node.getKey()).isEqualTo(key);
    }

    @Test
    void getValueNode() {
        Node valueNode = mock(Node.class);
        PairNode node = new PairNode("foo", valueNode);

        assertThat(node.getValue()).isEqualTo(valueNode);
    }

}