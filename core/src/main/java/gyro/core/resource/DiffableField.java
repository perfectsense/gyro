package gyro.core.resource;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.CaseFormat;
import com.psddev.dari.util.ConversionException;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.Scope;

public class DiffableField {

    private final String name;
    private final Method getter;
    private final Method setter;
    private final boolean updatable;
    private final String testValue;
    private final boolean testValueRandomSuffix;
    private final boolean collection;
    private final Class<?> itemClass;

    protected DiffableField(String javaName, Method getter, Method setter, Type type) {
        this.name = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, javaName);
        this.getter = getter;
        this.setter = setter;
        this.updatable = getter.isAnnotationPresent(Updatable.class);

        Output output = getter.getAnnotation(Output.class);

        if (output != null) {
            this.testValue = !ObjectUtils.isBlank(output.value()) ? output.value() : name;
            this.testValueRandomSuffix = output.randomSuffix();

        } else {
            this.testValue = null;
            this.testValueRandomSuffix = false;
        }

        if (type instanceof Class) {
            this.collection = false;
            this.itemClass = (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            this.collection = Collection.class.isAssignableFrom((Class<?>) parameterizedType.getRawType());
            this.itemClass = Optional.of(parameterizedType)
                .map(ParameterizedType::getActualTypeArguments)
                .filter(args -> args.length > 0)
                .map(args -> args[0])
                .filter(a0 -> a0 instanceof Class)
                .map(Class.class::cast)
                .orElseThrow(UnsupportedOperationException::new);

        } else {
            throw new GyroException(String.format(
                "@|bold %s|@ isn't supported as a field type!",
                type.getTypeName()));
        }
    }

    public String getName() {
        return name;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public String getTestValue() {
        return testValue;
    }

    public boolean isTestValueRandomSuffix() {
        return testValueRandomSuffix;
    }

    public boolean isCollection() {
        return collection;
    }

    public Class<?> getItemClass() {
        return itemClass;
    }

    @SuppressWarnings("unchecked")
    public boolean shouldBeDiffed() {
        return Diffable.class.isAssignableFrom(itemClass)
            && !DiffableType.getInstance((Class<? extends Diffable>) itemClass).isRoot();
    }

    public Object getValue(Diffable diffable) {
        DiffableScope scope = DiffableInternals.getScope(diffable);

        if (scope != null) {
            Object value = scope.get(name);

            if (value != null) {
                return value;
            }
        }

        return Reflections.invoke(getter, diffable);
    }

    public void setValue(Diffable diffable, Object value) {
        Scope scope = diffable.scope;
        Type type = setter.getGenericParameterTypes()[0];

        try {
            if (value instanceof Collection
                    && !Collection.class.isAssignableFrom(setter.getParameterTypes()[0])) {

                value = ((Collection<?>) value).stream()
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            }

            Reflections.invoke(setter, diffable, scope.getRootScope().convertValue(type, value));

        } catch (ConversionException error) {
            throw new GyroException(
                scope.getKeyNodes().get(name),
                String.format("Can't set @|bold %s|@ to @|bold %s|@ because it can't be converted to an instance of @|bold %s|@!",
                    name,
                    value,
                    type.getTypeName()));
        }
    }

}
