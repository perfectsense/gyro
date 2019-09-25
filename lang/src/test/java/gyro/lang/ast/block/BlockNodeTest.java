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