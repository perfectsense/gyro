package gyro.lang.ast.control;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class IfNodeTest extends AbstractNodeTest<IfNode> {

    @Test
    void constructorContext() {
        IfNode node = (IfNode) Node.parse("if 0\nelse if 0\nelse\nend", GyroParser::ifStatement);
        List<Node> conditions = node.getConditions();
        List<List<Node>> bodies = node.getBodies();

        assertThat(conditions).hasSize(2);
        assertThat(bodies).hasSize(3);
        bodies.forEach(body -> assertThat(body).isEmpty());
    }

    @Test
    void getConditions() {
        Node condition0 = mock(Node.class);
        Node condition1 = mock(Node.class);
        IfNode node = new IfNode(Arrays.asList(condition0, condition1), Collections.emptyList());

        assertThat(node.getConditions()).containsExactly(condition0, condition1);
    }

    @Test
    void getConditionsImmutable() {
        IfNode node = new IfNode(Collections.emptyList(), Collections.emptyList());

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> node.getConditions().add(mock(Node.class)));
    }

    @Test
    void getBodies() {
        Node body0 = mock(Node.class);
        Node body1 = mock(Node.class);
        Node body2 = mock(Node.class);

        IfNode node = new IfNode(
            Collections.emptyList(),
            Arrays.asList(
                Collections.singletonList(body0),
                Arrays.asList(body1, body2)));

        List<List<Node>> bodies = node.getBodies();

        assertThat(bodies).hasSize(2);
        assertThat(bodies.get(0)).containsExactly(body0);
        assertThat(bodies.get(1)).containsExactly(body1, body2);
    }

    @Test
    void getBodiesImmutable() {
        IfNode node = new IfNode(Collections.emptyList(), Collections.singletonList(Collections.emptyList()));

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> node.getBodies().add(Collections.emptyList()));

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> node.getBodies().get(0).add(mock(Node.class)));
    }

}