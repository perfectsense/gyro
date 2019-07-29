package gyro.core.scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import gyro.core.FileBackend;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScopeTest {

    @Test
    void constructor() {
        Scope scope = new Scope(null);

        assertThat(scope.getParent()).isNull();
        assertThat(scope).isEmpty();
    }

    @Test
    void getSettingsNull() {
        assertThatNullPointerException()
            .isThrownBy(() -> new Scope(null).getSettings(null));
    }

    @Test
    void getSettings() {
        Scope scope = new Scope(null);

        assertThat(scope.getSettings(TestSettings.class))
            .isNotNull()
            .isEqualTo(scope.getSettings(TestSettings.class));
    }

    @Test
    void addValueNull() {
        Scope scope = new Scope(null);

        scope.addValue("foo", null, "bar");

        assertThat(scope.get("foo"))
            .asList()
            .containsExactly("bar");
    }

    @Test
    void addValueList() {
        Scope scope = new Scope(null);

        scope.put("foo", new ArrayList<>(Collections.singletonList("bar")));
        scope.addValue("foo", null, "qux");

        assertThat(scope.get("foo"))
            .asList()
            .containsExactly("bar", "qux");
    }

    @Test
    void addValueScalar() {
        Scope scope = new Scope(null);

        scope.put("foo", "bar");
        scope.addValue("foo", null, "qux");

        assertThat(scope.get("foo"))
            .asList()
            .containsExactly("bar", "qux");
    }

    @Nested
    class Hierarchy {

        RootScope root;
        FileScope file;
        Scope scope;

        @BeforeEach
        void beforeEach() {
            root = new RootScope("", mock(FileBackend.class), null, null);
            file = new FileScope(root, "");
            scope = new Scope(file);
        }

        @Test
        void getParentNull() {
            assertThat(root.getParent()).isNull();
        }

        @Test
        void getParent() {
            assertThat(scope.getParent()).isEqualTo(file);
        }

        @Test
        void getClosestNull() {
            assertThat(scope.getClosest(DiffableScope.class)).isNull();
        }

        @Test
        void getClosest() {
            assertThat(scope.getClosest(FileScope.class)).isEqualTo(file);
        }

        @Test
        void getRootScope() {
            assertThat(scope.getRootScope()).isEqualTo(root);
        }

        @Test
        void getFileScope() {
            assertThat(scope.getFileScope()).isEqualTo(file);
        }

    }

    @Nested
    class Delegate {

        Map<String, Object> map;
        Scope scope;

        @BeforeEach
        @SuppressWarnings("unchecked")
        void beforeEach() {
            map = mock(Map.class);
            scope = new Scope(null, map);
        }

        @Test
        void clear() {
            scope.clear();

            verify(map).clear();
            verifyNoMoreInteractions(map);
        }

        @Test
        void containsKey() {
            Object key = new Object();

            scope.containsKey(key);

            verify(map).containsKey(key);
            verifyNoMoreInteractions(map);
        }

        @Test
        void containsValue() {
            Object value = new Object();

            scope.containsValue(value);

            verify(map).containsValue(value);
            verifyNoMoreInteractions(map);
        }

        @Test
        void entrySet() {
            scope.entrySet();

            verify(map).entrySet();
            verifyNoMoreInteractions(map);
        }

        @Test
        void keySet() {
            scope.keySet();

            verify(map).keySet();
            verifyNoMoreInteractions(map);
        }

        @Test
        void get() {
            Object key = new Object();

            scope.get(key);

            verify(map).get(key);
            verifyNoMoreInteractions(map);
        }

        @Test
        void isEmpty() {
            scope.isEmpty();

            verify(map).isEmpty();
            verifyNoMoreInteractions(map);
        }

        @Test
        void put() {
            String key = "foo";
            Object value = new Object();

            scope.put(key, value);

            verify(map).put(key, value);
            verifyNoMoreInteractions(map);
        }

        @Test
        @SuppressWarnings("unchecked")
        void putAll() {
            Map<String, Object> other = mock(Map.class);

            scope.putAll(other);

            verify(map).putAll(other);
            verifyNoMoreInteractions(other);
        }

        @Test
        void remove() {
            Object key = new Object();

            scope.remove(key);

            verify(map).remove(key);
            verifyNoMoreInteractions(map);
        }

        @Test
        void size() {
            scope.size();

            verify(map).size();
            verifyNoMoreInteractions(map);
        }

        @Test
        void values() {
            scope.values();

            verify(map).values();
            verifyNoMoreInteractions(map);
        }

    }

}