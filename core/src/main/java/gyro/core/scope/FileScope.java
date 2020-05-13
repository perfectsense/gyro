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

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.Resource;

public class FileScope extends Scope {

    private final String file;

    public FileScope(RootScope parent, String file) {
        super(parent);

        this.file = Preconditions.checkNotNull(file);
    }

    public String getFile() {
        return file;
    }

    @Override
    public void clear() {
        if (!(this instanceof RootScope) && getRootScope() != null) {
            keySet().forEach(k -> getRootScope().getResources().remove(k));
        }

        super.clear();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new AbstractSet<Entry<String, Object>>() {

            @Override
            public void clear() {
                FileScope.this.clear();
            }

            @Override
            public Iterator<Map.Entry<String, Object>> iterator() {
                return FileScope.super.entrySet().iterator();
            }

            @Override
            public int size() {
                return FileScope.super.entrySet().size();
            }
        };
    }

    @Override
    public Set<String> keySet() {
        return new AbstractSet<String>() {

            @Override
            public void clear() {
                FileScope.this.clear();
            }

            @Override
            public Iterator<String> iterator() {
                return FileScope.super.keySet().iterator();
            }

            @Override
            public int size() {
                return FileScope.super.keySet().size();
            }
        };
    }

    @Override
    public Object put(String key, Object value) {
        if (!(this instanceof RootScope) && getRootScope() != null && value instanceof Resource) {
            getRootScope().getResources().put(key, (Resource) value);
        }

        return super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ?> other) {
        if (!(this instanceof RootScope) && getRootScope() != null) {
            other.forEach((key, value) -> {
                if (value instanceof Resource) {
                    getRootScope().getResources().put(key, (Resource) value);
                }
            });
        }

        super.putAll(other);
    }

    @Override
    public Object remove(Object key) {
        if (!(this instanceof RootScope) && getRootScope() != null) {
            getRootScope().getResources().remove(key);
        }

        return super.remove(key);
    }

    @Override
    public Collection<Object> values() {
        return new AbstractCollection<Object>() {

            @Override
            public void clear() {
                FileScope.this.clear();
            }

            @Override
            public Iterator<Object> iterator() {
                return FileScope.super.values().iterator();
            }

            @Override
            public int size() {
                return FileScope.super.values().size();
            }
        };
    }

    FileScope copyWorkflowOnlyFileScope(RootScope parent) {
        FileScope scope = new FileScope(parent, getFile());

        for (Map.Entry<String, Object> entry : entrySet()) {
            Object value = entry.getValue();

            if (value instanceof Resource && DiffableInternals.getModifiedIn((Resource) value) != null) {
                scope.put(entry.getKey(), value);
            }
        }
        return scope;
    }
}
