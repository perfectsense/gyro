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

package gyro.core.resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gyro.core.FileBackend;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.FileScope;
import gyro.core.scope.RootScope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;
import gyro.parser.antlr4.GyroParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExtendsDirectiveProcessorTest {

    ExtendsDirectiveProcessor processor;
    DiffableScope scope;

    @BeforeEach
    void beforeEach() {
        processor = new ExtendsDirectiveProcessor();
        scope = new DiffableScope(new FileScope(new RootScope("", mock(FileBackend.class), null, null), ""), null);
    }

    private DirectiveNode parse(String text) {
        return (DirectiveNode) Node.parse(text, GyroParser::directive);
    }

    @Test
    void mergeList() {
        scope.put("f", ImmutableList.of(1L, 2L, 3L));
        processor.process(scope, parse("@extends: { f: [ 4, 5 ] } -merge true"));

        assertThat(scope.get("f")).isEqualTo(ImmutableList.of(1L, 2L, 3L, 4L, 5L));
    }

    @Test
    void mergeMap() {
        scope.put("f", ImmutableMap.of("key1", "value1"));
        processor.process(scope, parse("@extends: { f: { key2: value2 } } -merge true"));

        assertThat(scope.get("f")).isEqualTo(ImmutableMap.of(
            "key1", "value1",
            "key2", "value2"));
    }

    @Test
    void mergeRecursively() {
        scope.put("f", ImmutableMap.of(
            "map", ImmutableMap.of(
                "key1", "value1",
                "list", ImmutableList.of(1L, 2L, 3L))));

        processor.process(scope, parse("@extends: { f: { map: { key2: value2, list: [ 4, 5 ] } } } -merge true"));

        assertThat(scope.get("f")).isEqualTo(ImmutableMap.of(
            "map", ImmutableMap.of(
                "key1", "value1",
                "key2", "value2",
                "list", ImmutableList.of(1L, 2L, 3L, 4L, 5L))));
    }

    @Test
    void exclude() {
        scope.put("d", "value_d");
        processor.process(scope, parse("@extends: { a: value_a, b: value_b, c: value_c } -exclude [a, b]"));

        assertThat(scope.get("a")).isNull();
        assertThat(scope.get("b")).isNull();
        assertThat(scope.get("c")).isEqualTo("value_c");
        assertThat(scope.get("d")).isEqualTo("value_d");
    }

}
