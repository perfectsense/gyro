package gyro.core.repo;

import java.util.Collections;
import java.util.List;

import gyro.core.FileBackend;
import gyro.core.GyroException;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.value.ValueNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RepositoryDirectiveProcessorTest {

    RepositoryDirectiveProcessor processor;

    @BeforeEach
    void beforeEach() {
        processor = new RepositoryDirectiveProcessor();
    }

    @Test
    void getName() {
        assertThat(processor.getName()).isEqualTo("repository");
    }

    @Test
    void processNotRootScope() {
        assertThatExceptionOfType(GyroException.class)
            .isThrownBy(() -> processor.process(new Scope(null), null));
    }

    @Nested
    class WithRootScope {

        RootScope root;

        @BeforeEach
        void beforeEach() {
            root = new RootScope(null, mock(FileBackend.class), null, null);
        }

        @Test
        void process() {
            String url = "https://example.com/foo";

            processor.process(root, new DirectiveNode(
                "repository",
                Collections.singletonList(new ValueNode(url)),
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()));

            List<RemoteRepository> repositories = root.getSettings(RepositorySettings.class).getRepositories();

            assertThat(repositories).hasSize(2);
            assertThat(repositories.get(0)).isEqualTo(RepositorySettings.CENTRAL);
            assertThat(repositories.get(1).getUrl()).isEqualTo(url);
        }
    }

}