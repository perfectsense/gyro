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

package gyro.core.scope;

import java.util.Arrays;
import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gyro.core.FileBackend;
import gyro.core.GyroException;
import gyro.core.reference.ReferenceSettings;
import gyro.lang.ast.Node;
import gyro.lang.ast.value.ReferenceNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class NodeEvaluatorTest {

    @Nested
    class Static {

        @Nested
        class GetValueTest {

            @Test
            void glob() {
                assertThat(NodeEvaluator.getValue(null, null, "*")).isInstanceOf(Collection.class);
            }

            @Test
            void list() {
                assertThat(NodeEvaluator.getValue(null, ImmutableList.of("foo", "bar"), "1")).isEqualTo("bar");
            }

            @Test
            void listNegativeIndex() {
                assertThat(NodeEvaluator.getValue(null, ImmutableList.of("foo", "bar"), "-1")).isEqualTo("bar");
            }

            @Test
            void listError() {
                assertThatExceptionOfType(GyroException.class)
                    .isThrownBy(() -> NodeEvaluator.getValue(null, ImmutableList.of("foo", "bar"), "2"));
            }

            @Test
            void map() {
                assertThat(NodeEvaluator.getValue(null, ImmutableMap.of("foo", "bar"), "foo")).isEqualTo("bar");
            }

            @Test
            void method() {
                assertThat(NodeEvaluator.getValue(null, ImmutableList.of("foo", "bar"), "size")).isEqualTo(2);
            }

        }

    }

    @Nested
    class Instance {

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
                assertThatExceptionOfType(Defer.class)
                    .isThrownBy(() -> evaluate("$($(foo))"));
            }

            @Test
            void string() {
                scope.put("foo", "bar");
                assertThat(evaluate("$(foo)")).isEqualTo("bar");
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
            class ValueTest {

                @BeforeEach
                void beforeEach() {
                    root.getSettings(ReferenceSettings.class).addResolver(ValueReferenceResolver.class);
                }

                @Test
                void resolveNull() {
                    assertThat(evaluate("$(value)")).isNull();
                }

                @Test
                void resolveString() {
                    assertThat(evaluate("$(value 'foo')")).isEqualTo("foo");
                }

            }

            @Nested
            class ResourceTest {

                @BeforeEach
                void beforeEach() {
                    root.put("test::resource", TestResource.class);
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
                    assertThatExceptionOfType(Defer.class)
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

}