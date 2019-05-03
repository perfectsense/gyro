package gyro.lang.ast;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import gyro.lang.GyroErrorListener;
import gyro.lang.GyroErrorStrategy;
import gyro.lang.GyroLanguageException;
import gyro.parser.antlr4.GyroLexer;
import gyro.parser.antlr4.GyroParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.Tree;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public abstract class AbstractNodeTest<N extends Node> {

    private static final ImmutableMap<Class<?>, Object> TEST_VALUES = ImmutableMap.<Class<?>, Object>builder()
        .put(String.class, "foo")
        .build();

    private final Class<?> nodeClass = (Class<?>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    public static <T extends Tree> T parse(String text, Function<GyroParser, T> function) {
        GyroLexer lexer = new GyroLexer(CharStreams.fromString(text));
        CommonTokenStream stream = new CommonTokenStream(lexer);
        GyroParser parser = new GyroParser(stream);
        GyroErrorListener errorListener = new GyroErrorListener();

        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        parser.setErrorHandler(new GyroErrorStrategy());

        T tree = function.apply(parser);

        int errorCount = errorListener.getSyntaxErrors();

        if (errorCount > 0) {
            throw new GyroLanguageException(String.format(
                "%d %s found while parsing.",
                errorCount,
                errorCount == 1 ? "error" : "errors"));
        }

        return tree;
    }

    @TestFactory
    public List<DynamicTest> constructorNull() {
        List<DynamicTest> tests = new ArrayList<>();

        for (Constructor<?> constructor : nodeClass.getConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();

            for (int i = 0, length = paramTypes.length; i < length; ++i) {
                final int index = i;

                tests.add(DynamicTest.dynamicTest(constructor.toGenericString(), () -> {
                    Object[] params = new Object[length];
                    Class<?> paramType = paramTypes[index];
                    params[index] = TEST_VALUES.get(paramType);

                    if (params[index] == null) {
                        params[index] = mock(paramType);
                    }

                    assertThatExceptionOfType(InvocationTargetException.class)
                        .isThrownBy(() -> constructor.newInstance(params))
                        .withCauseInstanceOf(NullPointerException.class);
                }));
            }
        }

        return tests;
    }

}
