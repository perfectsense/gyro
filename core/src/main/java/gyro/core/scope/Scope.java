package gyro.core.scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gyro.core.Reflections;
import gyro.lang.ast.Node;
import gyro.util.MapWrapper;

public class Scope extends MapWrapper<String, Object> {

    private final Scope parent;
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
                return s.get(key);
            }
        }

        throw new Defer(node, String.format(
            "Can't resolve @|bold %s|@!",
            key));
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

}
