package gyro.lang.ast.block;

import java.util.Collections;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResourceNodeTest extends AbstractNodeTest<ResourceNode> {

    @Test
    void constructorContext() {
        ResourceNode node = new ResourceNode((GyroParser.ResourceContext) parse("foo::bar qux\nend", GyroParser::block));
        Node name = node.getName();

        assertThat(node.getType()).isEqualTo("foo::bar");
        assertThat(name).isInstanceOf(ValueNode.class);
        assertThat(((ValueNode) name).getValue()).isEqualTo("qux");
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
        Node name = mock(Node.class);
        ResourceNode node = new ResourceNode("foo", name, Collections.emptyList());

        assertThat(node.getName()).isEqualTo(name);
    }

}