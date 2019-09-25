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

import java.util.Arrays;
import java.util.List;

import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.KeyBlockNode;
import gyro.lang.ast.block.ResourceNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.ReferenceNode;
import gyro.lang.ast.value.ValueNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.mockito.Mockito.*;

class NodeAcceptTest {

    @TestFactory
    List<DynamicTest> accept() {
        return Arrays.asList(
            create(DirectiveNode.class, NodeVisitor::visitDirective),
            create(PairNode.class, NodeVisitor::visitPair),
            create(FileNode.class, NodeVisitor::visitFile),
            create(KeyBlockNode.class, NodeVisitor::visitKeyBlock),
            create(ResourceNode.class, NodeVisitor::visitResource),
            create(InterpolatedStringNode.class, NodeVisitor::visitInterpolatedString),
            create(ListNode.class, NodeVisitor::visitList),
            create(MapNode.class, NodeVisitor::visitMap),
            create(ReferenceNode.class, NodeVisitor::visitReference),
            create(ValueNode.class, NodeVisitor::visitValue)
        );
    }

    <N extends Node> DynamicTest create(Class<N> nodeClass, Verifier<N> verifier) {
        return DynamicTest.dynamicTest(nodeClass.getName(), () -> {
            @SuppressWarnings("unchecked")
            NodeVisitor<Object, Object, RuntimeException> visitor = mock(NodeVisitor.class, CALLS_REAL_METHODS);
            N node = mock(nodeClass, CALLS_REAL_METHODS);

            visitor.visit(node, null);
            verify(visitor).visit(node, null);
            node.accept(visitor, null);
            verifier.verify(verify(visitor, times(2)), node, null);
            verifyNoMoreInteractions(visitor);
        });
    }

    @FunctionalInterface
    interface Verifier<N extends Node> {

        void verify(NodeVisitor<Object, Object, RuntimeException> visitor, N node, Object context);

    }

}