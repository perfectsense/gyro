package gyro.core.scope;

import java.util.ArrayList;
import java.util.Collections;

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
            root = new RootScope("", mock(FileBackend.class), null);
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

}