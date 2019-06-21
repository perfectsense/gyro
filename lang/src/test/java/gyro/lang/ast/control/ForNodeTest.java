package gyro.lang.ast.control;

import java.util.Arrays;
import java.util.Collections;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.lang.ast.value.ListNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ForNodeTest extends AbstractNodeTest<ForNode> {

    @Test
    void constructorContext() {
        ForNode node = (ForNode) Node.parse("for foo in ['bar']\nend", GyroParser::forStatement);

        assertThat(node.getVariables()).containsExactly("foo");
        assertThat(node.getValue()).isInstanceOf(ListNode.class);
        assertThat(node.getBody()).hasSize(0);
    }

    @Test
    void getVariables() {
        String var0 = "foo";
        String var1 = "bar";
        ForNode node = new ForNode(Arrays.asList(var0, var1), mock(Node.class), Collections.emptyList());

        assertThat(node.getVariables()).containsExactly(var0, var1);
    }

    @Test
    void getValue() {
        Node value = mock(Node.class);
        ForNode node = new ForNode(Collections.emptyList(), value, Collections.emptyList());

        assertThat(node.getValue()).isSameAs(value);
    }

}