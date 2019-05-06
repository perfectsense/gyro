package gyro.lang.ast.block;

import java.util.Arrays;
import java.util.Collections;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class BlockNodeTest extends AbstractNodeTest<TestBlockNode> {

    @Test
    void getBody() {
        Node item0 = mock(Node.class);
        Node item1 = mock(Node.class);
        BlockNode node = new TestBlockNode(Arrays.asList(item0, item1));

        assertThat(node.getBody()).containsExactly(item0, item1);
    }

    @Test
    void getBodyImmutable() {
        BlockNode node = new TestBlockNode(Collections.emptyList());

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> node.getBody().add(mock(Node.class)));
    }

}