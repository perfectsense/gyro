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

package gyro.core.finder;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.CaseFormat;
import com.psddev.dari.util.Converter;
import gyro.core.GyroException;
import gyro.core.Reflections;

public class FinderField {

    private static final Converter CONVERTER;

    static {
        CONVERTER = new Converter();
        CONVERTER.setThrowError(true);
        CONVERTER.putAllStandardFunctions();
    }

    private final String javaName;
    private final String gyroName;
    private final Method getter;
    private final Method setter;
    private final String filterName;
    private final Class<?> itemClass;

    protected FinderField(String javaName, Method getter, Method setter, Type type) {
        this.javaName = javaName;
        this.gyroName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, javaName);
        this.getter = getter;
        this.setter = setter;

        Filter filterable = getter.getAnnotation(Filter.class);
        if (filterable != null) {
            this.filterName = filterable.value();
        } else {
            this.filterName = getGyroName();
        }

        if (type instanceof Class) {
            this.itemClass = (Class<?>) type;

        } else if (type instanceof ParameterizedType) {
            this.itemClass = Optional.of((ParameterizedType) type)
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

    public String getJavaName() {
        return javaName;
    }

    public String getGyroName() {
        return gyroName;
    }

    public Class<?> getItemClass() {
        return itemClass;
    }

    public String getFilterName() {
        return filterName;
    }

    public Object getValue(Finder finder) {
        return Reflections.invoke(getter, finder);
    }

    public void setValue(Finder finder, Object value) {
        if (value instanceof Collection
            && !Collection.class.isAssignableFrom(setter.getParameterTypes()[0])) {

            value = ((Collection<?>) value).stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        }

        Reflections.invoke(setter, finder, CONVERTER.convert(setter.getGenericParameterTypes()[0], value));
    }

}
