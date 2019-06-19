package gyro.core.scope;

import java.util.Collections;
import java.util.stream.Stream;

import gyro.core.FileBackend;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.condition.ValueConditionNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.ReferenceNode;
import gyro.lang.ast.value.ValueNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeEvaluatorTest {

    @TestFactory
    Stream<DynamicTest> visitValueConditionTrue() {
        NodeEvaluator evaluator = new NodeEvaluator();
        RootScope root = new RootScope("", mock(FileBackend.class), null, null);
        Scope scope = new Scope(root);

        scope.put("foo", "bar");

        return Stream.of(
            new InterpolatedStringNode(Collections.singletonList(
                new ReferenceNode(Collections.singletonList(new ValueNode("foo")), Collections.emptyList()))),
            new ListNode(Collections.singletonList(new ValueNode("bar"))),
            new MapNode(Collections.singletonList(new PairNode("qux", new ValueNode(14)))),
            new ValueNode(true),
            new ValueNode(41),
            new ValueNode("foo")
        )
            .map(node -> DynamicTest.dynamicTest(node.toString(), () -> {
                assertThat(evaluator.visitValueCondition(new ValueConditionNode(node), scope)).isEqualTo(Boolean.TRUE);
            }));
    }

    @TestFactory
    Stream<DynamicTest> visitValueConditionFalse() {
        NodeEvaluator evaluator = new NodeEvaluator();
        RootScope root = new RootScope("", mock(FileBackend.class), null, null);
        Scope scope = new Scope(root);

        scope.put("foo", "");

        return Stream.of(
            new InterpolatedStringNode(Collections.singletonList(
                new ReferenceNode(Collections.singletonList(new ValueNode("foo")), Collections.emptyList()))),
            new ListNode(Collections.emptyList()),
            new MapNode(Collections.emptyList()),
            new ValueNode(false),
            new ValueNode(0),
            new ValueNode("")
        )
            .map(node -> DynamicTest.dynamicTest(node.toString(), () -> {
                assertThat(evaluator.visitValueCondition(new ValueConditionNode(node), scope)).isEqualTo(Boolean.FALSE);
            }));
    }

}