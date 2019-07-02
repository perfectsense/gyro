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
            NodeVisitor<Object, Object> visitor = mock(NodeVisitor.class, CALLS_REAL_METHODS);
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

        void verify(NodeVisitor<Object, Object> visitor, N node, Object context);

    }

}