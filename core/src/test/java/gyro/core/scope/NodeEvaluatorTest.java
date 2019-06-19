package gyro.core.scope;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import gyro.core.FileBackend;
import gyro.core.GyroException;
import gyro.core.reference.ReferenceSettings;
import gyro.core.resource.DeferError;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.PairNode;
import gyro.lang.ast.condition.ValueConditionNode;
import gyro.lang.ast.value.InterpolatedStringNode;
import gyro.lang.ast.value.ListNode;
import gyro.lang.ast.value.MapNode;
import gyro.lang.ast.value.ReferenceNode;
import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeEvaluatorTest {

    NodeEvaluator evaluator;
    RootScope root;
    Scope scope;

    @BeforeEach
    void beforeEach() {
        evaluator = new NodeEvaluator();
        root = new RootScope("", mock(FileBackend.class), null, null);
        scope = new Scope(root);
    }

    @TestFactory
    Stream<DynamicTest> visitValueConditionTrue() {
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

    @Nested
    class ReferenceTest {

        @BeforeEach
        void beforeEach() {
        }

        private Object evaluate(String reference) {
            return evaluator.visitReference((ReferenceNode) Node.parse(reference, GyroParser::reference), scope);
        }

        @Test
        void noFirst() {
            assertThat(evaluate("$()")).isNull();
        }

        @Test
        void nullFirst() {
            assertThat(evaluate("$($(foo))")).isNull();
        }

        @Test
        void string() {
            scope.put("foo", "bar");
            assertThat(evaluate("$(foo)")).isEqualTo("bar");
        }

        @Test
        void map() {
            scope.put("foo", ImmutableMap.of("bar", "qux"));
            assertThat(evaluate("$(foo bar)")).isEqualTo("qux");
        }

        @Test
        void mapNull() {
            scope.put("foo", ImmutableMap.of());
            assertThat(evaluate("$(foo bar)")).isNull();
        }

        private void addConstantReferenceResolver(Object constant) {
            ConstantReferenceResolver r = new ConstantReferenceResolver(constant);
            root.getSettings(ReferenceSettings.class).getResolvers().put(r.getName(), r);
        }

        @Test
        void resolveNull() {
            addConstantReferenceResolver(null);
            assertThat(evaluate("$(constant)")).isNull();
        }

        @Test
        void resolveString() {
            addConstantReferenceResolver("foo");
            assertThat(evaluate("$(constant)")).isEqualTo("foo");
        }

        @Test
        void resolveMap() {
            addConstantReferenceResolver(ImmutableMap.of("foo", "bar"));
            assertThat(evaluate("$(constant foo)")).isEqualTo("bar");
        }

        @Test
        void resolveNested() {
            scope.put("foo", "bar");
            scope.put("bar", "qux");
            assertThat(evaluate("$($(foo))")).isEqualTo("qux");
        }

        @Test
        void query() {
            scope.put("foo", ImmutableMap.of("bar", "x"));
            assertThat(evaluate("$(foo | bar = 'x')")).isNotNull();
        }

        @Test
        void queryNull() {
            scope.put("foo", ImmutableMap.of("bar", "x"));
            assertThat(evaluate("$(foo | bar = 'y')")).isNull();
        }

        @Nested
        class ResourceTest {

            @BeforeEach
            void beforeEach() {
                root.getResourceClasses().put("test::resource", TestResource.class);
                root.put("test::resource::foo", new TestResource("foo"));
                root.put("test::resource::bar", new TestResource("bar"));
                root.put("test::resource::foobar", new TestResource("foobar"));
            }

            @Test
            void glob() {
                assertThat(evaluate("$(test::resource *)")).asList().hasSize(3);
            }

            @Test
            void globEnd() {
                assertThat(evaluate("$(test::resource foo*)")).asList().hasSize(2);
            }

            @Test
            void name() {
                assertThat(evaluate("$(test::resource foo)")).isInstanceOf(TestResource.class);
            }

            @Test
            void noResource() {
                assertThatExceptionOfType(DeferError.class)
                    .isThrownBy(() -> evaluate("$(test::resource qux)"));
            }

            @Test
            void noField() {
                assertThatExceptionOfType(GyroException.class)
                    .isThrownBy(() -> evaluate("$(test::resource foo bar)"));
            }

        }

        @Nested
        class QueryListTest {

            @BeforeEach
            void beforeEach() {
                scope.put("foo", Arrays.asList(
                    ImmutableMap.of(
                        "bar", "x",
                        "qux", "x"),
                    ImmutableMap.of(
                        "bar", "x",
                        "qux", "y"),
                    ImmutableMap.of(
                        "bar", "y",
                        "qux", "z")));
            }

            @Test
            void eq() {
                assertThat(evaluate("$(foo | bar = 'x')")).asList().hasSize(2);
            }

            @Test
            void ne() {
                assertThat(evaluate("$(foo | bar != 'x')")).asList().hasSize(1);
            }

            @Test
            void and() {
                assertThat(evaluate("$(foo | bar = 'x' and qux = 'y')")).asList().hasSize(1);
            }

            @Test
            void or() {
                assertThat(evaluate("$(foo | bar = 'x' or bar = 'y')")).asList().hasSize(3);
            }

            @Test
            void chain() {
                assertThat(evaluate("$(foo | bar = 'x' | qux = 'x')")).asList().hasSize(1);
            }

        }

    }

}