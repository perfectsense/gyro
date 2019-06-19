package gyro.lang.ast.value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gyro.lang.ast.AbstractNodeTest;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class InterpolatedStringNodeTest extends AbstractNodeTest<InterpolatedStringNode> {

    @Test
    void constructorArgument() {
        assertThatIllegalArgumentException()
            .isThrownBy(() -> new InterpolatedStringNode(Collections.singletonList(41)));
    }

    @Test
    void constructorContext() {
        InterpolatedStringNode node = new InterpolatedStringNode(
            (GyroParser.InterpolatedStringContext) parse("\"foo$(bar)qux\"", GyroParser::string));

        List<Object> items = node.getItems();

        assertThat(items).hasSize(3);
        assertThat(items.get(0)).isEqualTo("foo");
        assertThat(items.get(1)).isInstanceOf(ReferenceNode.class);
        assertThat(items.get(2)).isEqualTo("qux");
    }

    @Test
    void getItems() {
        String item0 = "foo";
        String item1 = "bar";
        InterpolatedStringNode node = new InterpolatedStringNode(Arrays.asList(item0, item1));

        assertThat(node.getItems()).containsExactly(item0, item1);
    }

    @Test
    void getItemsImmutable() {
        InterpolatedStringNode node = new InterpolatedStringNode(Collections.emptyList());

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> node.getItems().add("foo"));
    }

}