package gyro.lang.ast.condition;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class OrConditionNodeTest extends AbstractNodeTest<OrConditionNode> {

    @Test
    void constructorContext() {
        OrConditionNode node = new OrConditionNode(
            (GyroParser.OrConditionContext) parse("true or true", GyroParser::condition));

        Node left = node.getLeft();

        assertThat(left).isInstanceOf(ValueConditionNode.class);
        assertThat(((ValueConditionNode) left).getValue().getValue()).isEqualTo(Boolean.TRUE);

        Node right = node.getRight();

        assertThat(right).isInstanceOf(ValueConditionNode.class);
        assertThat(((ValueConditionNode) right).getValue().getValue()).isEqualTo(Boolean.TRUE);
    }

}