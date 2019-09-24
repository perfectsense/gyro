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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

class GlobCollection implements Collection<Object> {

    private final Collection<Object> collection;

    @SuppressWarnings("unchecked")
    public GlobCollection(Object value) {
        this.collection = value instanceof Collection
            ? Collections.unmodifiableCollection((Collection<Object>) value)
            : Collections.singletonList(value);
    }

    @Override
    public boolean add(Object item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<?> other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object item) {
        return collection.contains(item);
    }

    @Override
    public boolean containsAll(Collection<?> other) {
        return collection.containsAll(other);
    }

    @Override
    public boolean isEmpty() {
        return collection.isEmpty();
    }

    @Override
    public Iterator<Object> iterator() {
        return collection.iterator();
    }

    @Override
    public boolean remove(Object item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> other) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return collection.size();
    }

    @Override
    public Object[] toArray() {
        return collection.toArray();
    }

    @Override
    public <T> T[] toArray(T[] array) {
        return collection.toArray(array);
    }

}
