package gyro.core.resource;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import com.google.common.base.CaseFormat;
import com.psddev.dari.util.ConversionException;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.scope.Scope;

public class DiffableField {

    private final String name;
    private final Method getter;
    private final Method setter;
    private final boolean updatable;
    private final boolean collection;
    private final Class<?> itemClass;

    protected DiffableField(String javaName, Method getter, Method setter, Type type) {
        this.name = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, javaName);
        this.getter = getter;
        this.setter = setter;
        this.updatable = getter.isAnnotationPresent(Updatable.class);

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

    public void testUpdate(Diffable diffable) {
        if (!getter.isAnnotationPresent(Output.class)) {
            return;
        }

        Optional<Object> testValue = Optional.ofNullable(getter.getAnnotation(TestValue.class)).map(TestValue::value);

        if (Date.class.isAssignableFrom(itemClass)) {
            setValue(diffable, testValue.orElseGet(Date::new));

        } else if (Number.class.isAssignableFrom(itemClass)) {
            setValue(diffable, testValue.orElseGet(() -> Math.random() * Integer.MAX_VALUE));

        } else if (String.class.isAssignableFrom(itemClass)) {
            setValue(diffable, testValue.orElseGet(() -> String.join(
                "-",
                "test",
                name,
                UUID.randomUUID().toString().replace("-", "").substring(16))));
        }
    }

}
