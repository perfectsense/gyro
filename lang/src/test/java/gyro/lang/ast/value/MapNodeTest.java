package gyro.lang.ast.value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.PairNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapNodeTest extends AbstractNodeTest<MapNode> {

    @Test
    void constructorContext() {
        MapNode node = new MapNode(parse("{foo:1,bar:2}", GyroParser::map));
        List<PairNode> entries = node.getEntries();

        assertThat(entries).hasSize(2);
        entries.forEach(entry -> assertThat(entry.getValueNode()).isInstanceOf(NumberNode.class));
    }

    @Test
    void getEntries() {
        PairNode entry0 = mock(PairNode.class);
        PairNode entry1 = mock(PairNode.class);
        MapNode node = new MapNode(Arrays.asList(entry0, entry1));

        assertThat(node.getEntries()).containsExactly(entry0, entry1);
    }

    @Test
    void getEntriesImmutable() {
        MapNode node = new MapNode(Collections.emptyList());

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> node.getEntries().add(mock(PairNode.class)));
    }

}