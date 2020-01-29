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
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListNodeTest extends AbstractNodeTest<ListNode> {

    @Test
    void constructorContext() {
        ListNode node = (ListNode) Node.parse("['foo', 'bar']", GyroParser::list);
        List<Node> items = node.getItems();

        assertThat(items).hasSize(2);
        items.forEach(item -> assertThat(item).isInstanceOf(ValueNode.class));
        assertThat(((ValueNode) items.get(0)).getValue()).isEqualTo("foo");
        assertThat(((ValueNode) items.get(1)).getValue()).isEqualTo("bar");
    }

    @Test
    void getItems() {
        Node item0 = mock(Node.class);
        Node item1 = mock(Node.class);
        ListNode node = new ListNode(Arrays.asList(item0, item1));

        assertThat(node.getItems()).containsExactly(item0, item1);
    }

    @Test
    void getItemsImmutable() {
        ListNode node = new ListNode(Collections.emptyList());

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> node.getItems().add(mock(Node.class)));
    }

}