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
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class KeyBlockNodeTest extends AbstractNodeTest<KeyBlockNode> {

    @Test
    void constructorContext() {
        KeyBlockNode node = (KeyBlockNode) Node.parse("foo\nend", GyroParser::block);

        assertThat(node.getKey()).isEqualTo("foo");
        assertThat(node.getBody()).isEmpty();
    }

    @Test
    void getKey() {
        String key = "foo";
        KeyBlockNode node = new KeyBlockNode(key, null, Collections.emptyList());

        assertThat(node.getKey()).isEqualTo(key);
    }

}