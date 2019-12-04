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
        ResourceNode node = (ResourceNode) Node.parse("foo::bar qux\nend", GyroParser::block);
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
