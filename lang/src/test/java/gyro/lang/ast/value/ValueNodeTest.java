package gyro.lang.ast.value;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ValueNodeTest extends AbstractNodeTest<ValueNode> {

    @Test
    void constructorContextBoolean() {
        ValueNode node = (ValueNode) Node.parse("true", GyroParser::value);

        assertThat(node.getValue()).isEqualTo(Boolean.TRUE);
    }

    @Test
    void constructorContextNumber() {
        ValueNode node = (ValueNode) Node.parse("41", GyroParser::value);

        assertThat(node.getValue()).isEqualTo(41);
    }

    @Test
    void constructorContextString() {
        ValueNode node = (ValueNode) Node.parse("'foo'", GyroParser::value);

        assertThat(node.getValue()).isEqualTo("foo");
    }

    @Test
    void getValue() {
        String value = "foo";
        ValueNode node = new ValueNode(value);

        assertThat(node.getValue()).isEqualTo(value);
    }

}