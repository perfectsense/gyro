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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gyro.core.Reflections;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.lang.ast.Node;
import gyro.util.MapWrapper;

public class Scope extends MapWrapper<String, Object> {

    private Scope parent;
    private final Map<Object, String> names = new IdentityHashMap<>();
    private final Map<String, Node> locations = new HashMap<>();

    private final LoadingCache<Class<? extends Settings>, Settings> settingsByClass = CacheBuilder.newBuilder()
        .build(new CacheLoader<Class<? extends Settings>, Settings>() {

            @Override
            public Settings load(Class<? extends Settings> settingsClass) {
                Settings settings = Reflections.newInstance(settingsClass);
                settings.scope = Scope.this;

                return settings;
            }
        });

    /**
     * @param parent Nullable.
     * @param values Nullable.
     */
    public Scope(Scope parent, Map<String, Object> values) {
        super(values != null ? values : new LinkedHashMap<>());
        this.parent = parent;
    }

    /**
     * @param parent Nullable.
     */
    public Scope(Scope parent) {
        this(parent, null);
    }

    public Scope getParent() {
        return parent;
    }

    public void setParent(Scope parent) {
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    public <S extends Scope> S getClosest(Class<S> scopeClass) {
        for (Scope s = this; s != null; s = s.getParent()) {
            if (scopeClass.isInstance(s)) {
                return (S) s;
            }
        }

        return null;
    }

    public RootScope getRootScope() {
        return getClosest(RootScope.class);
    }

    public FileScope getFileScope() {
        return getClosest(FileScope.class);
    }

    public Object find(Node node, String key) {
        for (Scope s = this; s != null; s = s.parent) {
            if (s.containsKey(key)) {
                Object value = s.get(key);
                if (value instanceof Diffable && !(value instanceof Resource)) {
                    Object evaluatedObject = getObject((Diffable) value);
                    if (evaluatedObject != null) {
                        value = evaluatedObject;
                    }
                }

                return value;
            }
        }

        throw new Defer(node, String.format(
            "Can't resolve @|bold %s|@!",
            key));
    }

    /**
     * Searches for a Diffable in it's parent for updated values in case there were any changes to the state.
     */
    private Object getObject(Diffable diffableObject) {
        Object object = null;
        if (DiffableType.getInstance(diffableObject.getClass()).getFields().stream().anyMatch(
            DiffableField::isOutput)) {
            Diffable parent = diffableObject.parent();
            if (parent != null) {
                List<String> parentFields = DiffableType.getInstance(parent.getClass())
                    .getFields().stream().map(DiffableField::getName).collect(Collectors.toList());
                for (String field : parentFields) {
                    object = getObject(diffableObject, parent, field);
                    if (object != null) {
                        break;
                    }
                }
            }
        }

        return object;
    }

    @SuppressWarnings("rawtypes")
    private Object getObject(Diffable diffableObject, Diffable parent, String field) {
        Object value = Optional.ofNullable(DiffableType.getInstance(parent.getClass())
            .getField(field)).map(f -> f.getValue(parent)).orElse(null);
        Object evaluatedObject = null;

        if (value instanceof List) {
            for (Object objectFromParent : (ArrayList) value) {
                evaluatedObject = getObject(diffableObject, objectFromParent);
                if (evaluatedObject != null) {
                    break;
                }
            }

        } else if (value instanceof Set) {
            for (Object objectFromParent : (HashSet) value) {
                evaluatedObject = getObject(diffableObject, objectFromParent);
                if (evaluatedObject != null) {
                    break;
                }
            }

        } else {
            evaluatedObject = getObject(diffableObject, value);
        }

        return evaluatedObject;
    }

    private Object getObject(Diffable diffable, Object objectFromParent) {
        Object object = null;
        if (objectFromParent instanceof Diffable) {
            Diffable diffableFromParent = (Diffable) objectFromParent;
            if (diffableFromParent.primaryKey().equals(diffable.primaryKey()) && diffableFromParent.getClass()
                .equals(diffable.getClass())) {
                object = diffableFromParent;
            }
        }

        return object;
    }

    @SuppressWarnings("unchecked")
    public void addValue(String key, String name, Object value) {
        Object oldValue = get(key);
        List<Object> list;

        if (oldValue == null) {
            list = new ArrayList<>();

        } else if (oldValue instanceof List) {
            list = (List<Object>) oldValue;

        } else {
            list = new ArrayList<>();
            list.add(oldValue);
        }

        list.add(value);
        put(key, list);
        names.put(value, name);
    }

    public String getName(Object value) {
        return names.get(value);
    }

    public Node getLocation(String key) {
        return locations.get(key);
    }

    public void putLocation(String key, Node node) {
        locations.put(key, node);
    }

    @SuppressWarnings("unchecked")
    public <S extends Settings> S getSettings(Class<S> settingsClass) {
        return (S) settingsByClass.getUnchecked(Preconditions.checkNotNull(settingsClass));
    }

    public LoadingCache<Class<? extends Settings>, Settings> getSettingsByClass() {
        return settingsByClass;
    }

}
