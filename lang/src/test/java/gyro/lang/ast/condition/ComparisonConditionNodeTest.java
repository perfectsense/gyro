package gyro.lang.ast.condition;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.lang.ast.value.LiteralStringNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComparisonConditionNodeTest extends AbstractNodeTest<ComparisonConditionNode> {

    @Test
    void constructorContext() {
        ComparisonConditionNode node = new ComparisonConditionNode(
            (GyroParser.ComparisonConditionContext) parse("'foo' = 'bar'", GyroParser::condition));

        Node leftNode = node.getLeftNode();
        Node rightNode = node.getRightNode();

        assertThat(leftNode).isInstanceOf(LiteralStringNode.class);
        assertThat(((LiteralStringNode) leftNode).getValue()).isEqualTo("foo");
        assertThat(node.getOperator()).isEqualTo("=");
        assertThat(rightNode).isInstanceOf(LiteralStringNode.class);
        assertThat(((LiteralStringNode) rightNode).getValue()).isEqualTo("bar");
    }

    @Test
    void getLeftNode() {
        Node leftNode = mock(Node.class);
        ComparisonConditionNode node = new ComparisonConditionNode(leftNode, "=", mock(Node.class));

        assertThat(node.getLeftNode()).isEqualTo(leftNode);
    }

    @Test
    void getOperator() {
        String operator = "=";
        ComparisonConditionNode node = new ComparisonConditionNode(mock(Node.class), operator, mock(Node.class));

        assertThat(node.getOperator()).isEqualTo(operator);
    }

    @Test
    void getRightNode() {
        Node rightNode = mock(Node.class);
        ComparisonConditionNode node = new ComparisonConditionNode(mock(Node.class), "=", rightNode);

        assertThat(node.getRightNode()).isEqualTo(rightNode);
    }

}