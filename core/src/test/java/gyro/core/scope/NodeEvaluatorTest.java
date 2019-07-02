package gyro.core.scope;

import java.util.Arrays;

import com.google.common.collect.ImmutableMap;
import gyro.core.FileBackend;
import gyro.core.GyroException;
import gyro.core.reference.ReferenceSettings;
import gyro.core.resource.DeferError;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.core.resource.ValueReferenceException;
import gyro.lang.ast.Node;
import gyro.lang.ast.value.ReferenceNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
            assertThatExceptionOfType(ValueReferenceException.class)
                .isThrownBy(() -> evaluate("$($(foo))"));
        }

        @Test
        void string() {
            scope.put("foo", "bar");
            assertThat(evaluate("$(foo)")).isEqualTo("bar");
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
        void filter() {
            scope.put("foo", ImmutableMap.of("bar", "x"));
            assertThat(evaluate("$(foo | bar = 'x')")).isNotNull();
        }

        @Test
        void filterNull() {
            scope.put("foo", ImmutableMap.of("bar", "x"));
            assertThat(evaluate("$(foo | bar = 'y')")).isNull();
        }

        @Nested
        class ResourceTest {

            @BeforeEach
            void beforeEach() {
                root.put("test::resource", TestResource.class);
                root.addResource(new TestResource("foo"));
                root.addResource(new TestResource("bar"));
                root.addResource(new TestResource("foobar"));
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
        class FilterListTest {

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