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

class ListNodeTest extends AbstractNodeTest<ListNode> {

    @Test
    void constructorContext() {
        ListNode node = new ListNode(parse("['foo', 'bar']", GyroParser::list));
        List<Node> items = node.getItems();

        assertThat(items).hasSize(2);
        items.forEach(item -> assertThat(item).isInstanceOf(LiteralStringNode.class));
        assertThat(((LiteralStringNode) items.get(0)).getValue()).isEqualTo("foo");
        assertThat(((LiteralStringNode) items.get(1)).getValue()).isEqualTo("bar");
    }

    @Test
    void getItems() {
        Node item0 = mock(Node.class);
        Node item1 = mock(Node.class);
        ListNode node = new ListNode(Arrays.asList(item0, item1));

        assertThat(node.getItems()).containsExactly(item0, item1);
    }

    @Test
    void getItemsImmutable() {
        ListNode node = new ListNode(Collections.emptyList());

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> node.getItems().add(mock(Node.class)));
    }

}