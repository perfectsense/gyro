package gyro.lang.ast;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public abstract class AbstractNodeTest<N extends Node> {

    private static final ImmutableMap<Class<?>, Object> TEST_VALUES = ImmutableMap.<Class<?>, Object>builder()
        .put(String.class, "foo")
        .build();

    @SuppressWarnings("unchecked")
    private final Class<N> nodeClass = (Class<N>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];

    private static Object getTestValue(Class<?> c) {
        Object v = TEST_VALUES.get(c);
        return v != null ? v : mock(c);
    }

    @TestFactory
    public List<DynamicTest> constructorNull() {
        List<DynamicTest> tests = new ArrayList<>();

        for (Constructor<?> constructor : nodeClass.getConstructors()) {
            Class<?>[] paramTypes = constructor.getParameterTypes();

            for (int i = 0, length = paramTypes.length; i < length; ++i) {
                StringBuilder name = new StringBuilder("(");
                Object[] params = new Object[length];

                for (int j = 0; j < length; ++j) {
                    Class<?> paramType = paramTypes[j];

                    name.append(paramType.getName());

                    if (i == j) {
                        name.append("=null");

                    } else {
                        params[j] = getTestValue(paramType);
                    }

                    name.append(", ");
                }

                name.setLength(name.length() - 2);
                name.append(")");

                tests.add(DynamicTest.dynamicTest(
                    name.toString(),
                    () -> assertThatExceptionOfType(InvocationTargetException.class)
                        .isThrownBy(() -> constructor.newInstance(params))
                        .withCauseInstanceOf(NullPointerException.class)
                ));
            }
        }

        return tests;
    }

}
