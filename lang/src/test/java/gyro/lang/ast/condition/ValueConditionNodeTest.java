package gyro.lang.ast.condition;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ValueConditionNodeTest extends AbstractNodeTest<ValueConditionNode> {

    @Test
    void constructorContext() {
        ValueConditionNode node = new ValueConditionNode(
            (GyroParser.ValueConditionContext) parse("true", GyroParser::condition));

        assertThat(node.getValue().getValue()).isEqualTo(Boolean.TRUE);
    }

    @Test
    void getValue() {
        ValueNode value = mock(ValueNode.class);
        ValueConditionNode node = new ValueConditionNode(value);

        assertThat(node.getValue()).isEqualTo(value);
    }

}