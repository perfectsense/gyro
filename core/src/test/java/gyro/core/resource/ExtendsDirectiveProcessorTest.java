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

}