package gyro.lang.ast.block;

import java.util.List;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.lang.ast.PairNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class FileNodeTest extends AbstractNodeTest<FileNode> {

    @Test
    void constructorContext() {
        FileNode node = (FileNode) Node.parse("foo::bar qux\nend\nfoo: 'bar'", GyroParser::file);
        List<Node> body = node.getBody();

        assertThat(body).hasSize(2);
        assertThat(body.get(0)).isInstanceOf(ResourceNode.class);
        assertThat(body.get(1)).isInstanceOf(PairNode.class);
    }

}