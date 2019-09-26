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

import java.util.Collections;
import java.util.List;

import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectiveNodeTest extends AbstractNodeTest<DirectiveNode> {

    @Test
    void constructorContext() {
        DirectiveNode node = (DirectiveNode) Node.parse("@foo: 'bar'", GyroParser::directive);
        List<Node> arguments = node.getArguments();

        assertThat(node.getName()).isEqualTo("foo");
        assertThat(arguments).hasSize(1);
        assertThat(((ValueNode) arguments.get(0)).getValue()).isEqualTo("bar");
    }

    @Test
    void getName() {
        String name = "foo";
        DirectiveNode node = new DirectiveNode(
            name,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

        assertThat(node.getName()).isEqualTo(name);
    }

    @Test
    void getArguments() {
        ValueNode argument0 = mock(ValueNode.class);

        DirectiveNode node = new DirectiveNode(
            "foo",
            Collections.singletonList(argument0),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

        assertThat(node.getArguments()).containsExactly(argument0);
    }

    @Test
    void getArgumentsImmutable() {
        DirectiveNode node = new DirectiveNode(
            "foo",
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> node.getArguments().add(mock(ValueNode.class)));
    }

}