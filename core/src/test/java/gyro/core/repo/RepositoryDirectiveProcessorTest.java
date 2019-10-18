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

package gyro.core.repo;

import java.util.Collections;
import java.util.List;

import gyro.core.FileBackend;
import gyro.core.scope.RootScope;
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

    @Nested
    class WithRootScope {

        RootScope root;

        @BeforeEach
        void beforeEach() {
            root = new RootScope("", mock(FileBackend.class), null, null);
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

            assertThat(repositories).hasSize(3);
            assertThat(repositories.get(0)).isEqualTo(RepositorySettings.CENTRAL);
            assertThat(repositories.get(1)).isEqualTo(RepositorySettings.RELEASE);
            assertThat(repositories.get(2).getUrl()).isEqualTo(url);
        }
    }

}