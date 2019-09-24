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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gyro.core.Reflections;
import gyro.core.scope.Scope;

public class FinderType<F extends Finder> {

    private static final LoadingCache<Class<? extends Finder>, FinderType<? extends Finder>> INSTANCES = CacheBuilder
            .newBuilder()
            .build(new CacheLoader<Class<? extends Finder>, FinderType<? extends Finder>>() {

                @Override
                public FinderType<? extends Finder> load(Class<? extends Finder> finderClass) {
                    return new FinderType<>(finderClass);
                }
            });

    private final Class<F> finderClass;
    private final String name;
    private final List<FinderField> fields;
    private final Map<String, FinderField> fieldByJavaName;
    private final Map<String, FinderField> fieldByGyroName;

    @SuppressWarnings("unchecked")
    public static <F extends Finder> FinderType<F> getInstance(Class<F> finderClass) {
        return (FinderType<F>) INSTANCES.getUnchecked(finderClass);
    }

    private FinderType(Class<F> finderClass) {
        this.finderClass = finderClass;
        this.name = Reflections.getNamespace(finderClass) + "::" + Reflections.getType(finderClass);

        ImmutableList.Builder<FinderField> fields = ImmutableList.builder();
        ImmutableMap.Builder<String, FinderField> fieldByJavaName = ImmutableMap.builder();
        ImmutableMap.Builder<String, FinderField> fieldByGyroName = ImmutableMap.builder();

        for (PropertyDescriptor prop : Reflections.getBeanInfo(finderClass).getPropertyDescriptors()) {
            Method getter = prop.getReadMethod();
            Method setter = prop.getWriteMethod();

            if (getter != null && setter != null) {
                java.lang.reflect.Type getterType = getter.getGenericReturnType();
                java.lang.reflect.Type setterType = setter.getGenericParameterTypes()[0];

                if (getterType.equals(setterType)) {
                    FinderField field = new FinderField(prop.getName(), getter, setter, getterType);

                    fields.add(field);
                    fieldByJavaName.put(field.getJavaName(), field);
                    fieldByGyroName.put(field.getGyroName(), field);
                }
            }
        }

        this.fields = fields.build();
        this.fieldByJavaName = fieldByJavaName.build();
        this.fieldByGyroName = fieldByGyroName.build();
    }

    public String getName() {
        return name;
    }

    public List<FinderField> getFields() {
        return fields;
    }

    public FinderField getFieldByJavaName(String javaName) {
        return fieldByJavaName.get(javaName);
    }

    public FinderField getFieldByGyroName(String gyroName) {
        return fieldByGyroName.get(gyroName);
    }

    public F newInstance(Scope scope) {
        F finder = Reflections.newInstance(finderClass);
        finder.scope = scope;

        return finder;
    }

}
