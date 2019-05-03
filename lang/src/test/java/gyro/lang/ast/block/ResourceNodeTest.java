package gyro.lang.ast.block;

import java.util.Collections;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.lang.ast.value.LiteralStringNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceNodeTest extends AbstractNodeTest<ResourceNode> {

    @Test
    void constructorContext() {
        ResourceNode node = new ResourceNode(parse("foo::bar qux\nend", GyroParser::resource));
        Node nameNode = node.getNameNode();

        assertThat(node.getType()).isEqualTo("foo::bar");
        assertThat(nameNode).isInstanceOf(LiteralStringNode.class);
        assertThat(((LiteralStringNode) nameNode).getValue()).isEqualTo("qux");
        assertThat(node.getBody()).isEmpty();
    }

    @Test
    void getType() {
        String type = "foo";
        ResourceNode node = new ResourceNode(type, mock(Node.class), Collections.emptyList());

        assertThat(node.getType()).isEqualTo(type);
    }

    @Test
    void getNameNode() {
        Node nameNode = mock(Node.class);
        ResourceNode node = new ResourceNode("foo", nameNode, Collections.emptyList());

        assertThat(node.getNameNode()).isEqualTo(nameNode);
    }

}