package gyro.lang.ast.value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InterpolatedStringNodeTest extends AbstractNodeTest<InterpolatedStringNode> {

    @Test
    void constructorContext() {
        InterpolatedStringNode node = (InterpolatedStringNode) Node.parse("\"foo$(bar)qux\"", GyroParser::string);
        List<Node> items = node.getItems();

        assertThat(items).hasSize(3);
        assertThat(items.get(0)).isInstanceOf(ValueNode.class);
        assertThat(items.get(1)).isInstanceOf(ReferenceNode.class);
        assertThat(items.get(2)).isInstanceOf(ValueNode.class);
    }

    @Test
    void getItems() {
        Node item0 = mock(Node.class);
        Node item1 = mock(Node.class);
        InterpolatedStringNode node = new InterpolatedStringNode(Arrays.asList(item0, item1));

        assertThat(node.getItems()).containsExactly(item0, item1);
    }

    @Test
    void getItemsImmutable() {
        InterpolatedStringNode node = new InterpolatedStringNode(Collections.emptyList());

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> node.getItems().add(mock(Node.class)));
    }

}