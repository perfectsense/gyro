/*
 * Copyright 2019, Perfect Sense, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gyro.lang.ast.value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.lang.ast.PairNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapNodeTest extends AbstractNodeTest<MapNode> {

    @Test
    void constructorContext() {
        MapNode node = (MapNode) Node.parse("{foo:1,bar:2}", GyroParser::map);
        List<PairNode> entries = node.getEntries();

        assertThat(entries).hasSize(2);
        entries.forEach(entry -> assertThat(entry.getValue()).isInstanceOf(ValueNode.class));
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