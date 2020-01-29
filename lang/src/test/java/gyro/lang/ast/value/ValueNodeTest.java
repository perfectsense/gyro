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

import gyro.lang.ast.AbstractNodeTest;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ValueNodeTest extends AbstractNodeTest<ValueNode> {

    @Test
    void constructorContextBoolean() {
        ValueNode node = (ValueNode) Node.parse("true", GyroParser::value);

        assertThat(node.getValue()).isEqualTo(Boolean.TRUE);
    }

    @Test
    void constructorContextDouble() {
        ValueNode node = (ValueNode) Node.parse("41.41", GyroParser::value);

        assertThat(node.getValue()).isEqualTo(41.41);
    }

    @Test
    void constructorContextLong() {
        ValueNode node = (ValueNode) Node.parse("41", GyroParser::value);

        assertThat(node.getValue()).isEqualTo(41L);
    }

    @Test
    void constructorContextString() {
        ValueNode node = (ValueNode) Node.parse("'foo'", GyroParser::value);

        assertThat(node.getValue()).isEqualTo("foo");
    }

    @Test
    void getValue() {
        String value = "foo";
        ValueNode node = new ValueNode(value);

        assertThat(node.getValue()).isEqualTo(value);
    }

}
