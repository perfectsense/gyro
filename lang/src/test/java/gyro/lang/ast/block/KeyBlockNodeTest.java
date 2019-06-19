package gyro.lang.ast.block;

import java.util.Collections;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class KeyBlockNodeTest extends AbstractNodeTest<KeyBlockNode> {

    @Test
    void constructorContext() {
        KeyBlockNode node = (KeyBlockNode) Node.parse("foo\nend", GyroParser::block);

        assertThat(node.getKey()).isEqualTo("foo");
        assertThat(node.getBody()).isEmpty();
    }

    @Test
    void getKey() {
        String key = "foo";
        KeyBlockNode node = new KeyBlockNode(key, Collections.emptyList());

        assertThat(node.getKey()).isEqualTo(key);
    }

}