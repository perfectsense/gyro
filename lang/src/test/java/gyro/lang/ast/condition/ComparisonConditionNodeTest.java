package gyro.lang.ast.condition;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComparisonConditionNodeTest extends AbstractNodeTest<ComparisonConditionNode> {

    @Test
    void constructorContext() {
        ComparisonConditionNode node = new ComparisonConditionNode(
            (GyroParser.ComparisonConditionContext) parse("'foo' = 'bar'", GyroParser::condition));

        Node left = node.getLeft();
        Node right = node.getRight();

        assertThat(left).isInstanceOf(ValueNode.class);
        assertThat(((ValueNode) left).getValue()).isEqualTo("foo");
        assertThat(node.getOperator()).isEqualTo("=");
        assertThat(right).isInstanceOf(ValueNode.class);
        assertThat(((ValueNode) right).getValue()).isEqualTo("bar");
    }

    @Test
    void getLeftNode() {
        Node left = mock(Node.class);
        ComparisonConditionNode node = new ComparisonConditionNode(left, "=", mock(Node.class));

        assertThat(node.getLeft()).isEqualTo(left);
    }

    @Test
    void getOperator() {
        String operator = "=";
        ComparisonConditionNode node = new ComparisonConditionNode(mock(Node.class), operator, mock(Node.class));

        assertThat(node.getOperator()).isEqualTo(operator);
    }

    @Test
    void getRightNode() {
        Node right = mock(Node.class);
        ComparisonConditionNode node = new ComparisonConditionNode(mock(Node.class), "=", right);

        assertThat(node.getRight()).isEqualTo(right);
    }

}