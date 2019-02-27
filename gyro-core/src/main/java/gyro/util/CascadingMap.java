package gyro.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class CascadingMap<K, V> implements Map<K, V> {

    private final List<Map<K, V>> sources = new CopyOnWriteArrayList<>();

    public CascadingMap(Map<K, V>... sources) {
        if (sources != null) {
            Collections.addAll(this.sources, sources);
        }
    }

    // Combines all the sources, for when an unified view is required.
    private Map<K, V> combine() {

        // The listIterator method without index argument of sources.size() - 1
        // is used, because it's possible for the size to change between that
        // get and calling the listIterator.
        ListIterator<Map<K, V>> iterator = sources.listIterator();

        while (iterator.hasNext()) {
            iterator.next();
        }

        Map<K, V> combined = new HashMap<>();

        while (iterator.hasPrevious()) {
            combined.putAll(iterator.previous());
        }

        return Collections.unmodifiableMap(combined);
    }

    @Override
    public void clear() {
        sources.forEach(Map::clear);
    }

    @Override
    public boolean containsKey(Object key) {
        return sources.stream().anyMatch(s -> s.containsKey(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return combine().containsValue(value);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return combine().entrySet();
    }

    @Override
    public V get(Object key) {
        return sources.stream()
                .filter(s -> s.containsKey(key))
                .findFirst()
                .map(s -> s.get(key))
                .orElse(null);
    }

    @Override
    public boolean isEmpty() {
        for (Map<K, V> source : sources) {
            if (!source.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<K> keySet() {
        return combine().keySet();
    }

    private Map<K, V> getFirstSource() {
        if (sources.isEmpty()) {
            sources.add(new LinkedHashMap<>());
        }

        return sources.get(0);
    }

    @Override
    public V put(K key, V value) {
        V oldValue = get(key);

        getFirstSource().put(key, value);

        return oldValue;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        getFirstSource().putAll(map);
    }

    @Override
    public V remove(Object key) {
        V oldValue = get(key);

        for (Map<K, V> source : sources) {
            source.remove(key);
        }

        return oldValue;
    }

    @Override
    public int size() {
        return combine().size();
    }

    @Override
    public Collection<V> values() {
        return combine().values();
    }

    @Override
    public boolean equals(Object object) {
        return this == object || (object instanceof Map && combine().equals(object));
    }

    @Override
    public int hashCode() {
        return combine().hashCode();
    }

    @Override
    public String toString() {
        return combine().toString();
    }
}