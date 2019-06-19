package gyro.lang.ast.control;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForNodeTest extends AbstractNodeTest<ForNode> {

    @Test
    void constructorContext() {
        ForNode node = (ForNode) Node.parse("for foo in ['bar']\nend", GyroParser::forStatement);
        List<Node> items = node.getItems();

        assertThat(node.getVariables()).containsExactly("foo");
        assertThat(items).hasSize(1);
        assertThat(items.get(0)).isInstanceOf(ValueNode.class);
        assertThat(((ValueNode) items.get(0)).getValue()).isEqualTo("bar");
        assertThat(node.getBody()).hasSize(0);
    }

    @Test
    void getVariables() {
        String var0 = "foo";
        String var1 = "bar";
        ForNode node = new ForNode(Arrays.asList(var0, var1), Collections.emptyList(), Collections.emptyList());

        assertThat(node.getVariables()).containsExactly(var0, var1);
    }

    @Test
    void getItems() {
        Node item0 = mock(Node.class);
        Node item1 = mock(Node.class);
        ForNode node = new ForNode(Collections.emptyList(), Arrays.asList(item0, item1), Collections.emptyList());
        List<Node> items = node.getItems();

        assertThat(items).containsExactly(item0, item1);
    }

}