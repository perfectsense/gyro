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

package gyro.lang.ast;

import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PairNodeTest extends AbstractNodeTest<PairNode> {

    @Test
    void constructorContext() {
        PairNode node = (PairNode) Node.parse("foo: 'bar'", GyroParser::pair);
        Node keyNode = node.getKey();
        Node valueNode = node.getValue();

        assertThat(keyNode).isInstanceOf(ValueNode.class);
        assertThat(((ValueNode) keyNode).getValue()).isEqualTo("foo");
        assertThat(valueNode).isInstanceOf(ValueNode.class);
        assertThat(((ValueNode) valueNode).getValue()).isEqualTo("bar");
    }

    @Test
    void getKey() {
        Node keyNode = mock(Node.class);
        PairNode node = new PairNode(keyNode, mock(Node.class));

        assertThat(node.getKey()).isEqualTo(keyNode);
    }

    @Test
    void getValueNode() {
        Node valueNode = mock(Node.class);
        PairNode node = new PairNode(mock(Node.class), valueNode);

        assertThat(node.getValue()).isEqualTo(valueNode);
    }

}