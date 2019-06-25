package gyro.lang.ast.condition;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AndConditionNodeTest extends AbstractNodeTest<AndConditionNode> {

    @Test
    void constructorContext() {
        AndConditionNode node = (AndConditionNode) Node.parse("true and true", GyroParser::condition);
        Node left = node.getLeft();

        assertThat(left).isInstanceOf(ValueConditionNode.class);
        assertThat(getValue(left)).isEqualTo(Boolean.TRUE);

        Node right = node.getRight();

        assertThat(right).isInstanceOf(ValueConditionNode.class);
        assertThat(getValue(right)).isEqualTo(Boolean.TRUE);
    }

    private Object getValue(Node node) {
        return ((ValueNode) ((ValueConditionNode) node).getValue()).getValue();
    }

}