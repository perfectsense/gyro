package gyro.lang.ast.condition;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompoundConditionNodeTest extends AbstractNodeTest<TestCompoundConditionNode> {

    @Test
    void getLeft() {
        Node left = mock(Node.class);
        TestCompoundConditionNode node = new TestCompoundConditionNode(left, mock(Node.class));

        assertThat(node.getLeft()).isEqualTo(left);
    }

    @Test
    void getRight() {
        Node right = mock(Node.class);
        TestCompoundConditionNode node = new TestCompoundConditionNode(mock(Node.class), right);

        assertThat(node.getRight()).isEqualTo(right);
    }

}