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

package gyro.core.directive;

import gyro.core.FileBackend;
import gyro.core.scope.RootScope;
import gyro.util.Bug;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DirectivePluginTest {

    RootScope root;

    @BeforeEach
    void beforeEach() {
        root = new RootScope("", mock(FileBackend.class), null, null);
    }

    @Test
    void onEachClassPrivateDirectiveProcessor() {
        assertThatExceptionOfType(Bug.class)
            .isThrownBy(() -> new DirectivePlugin().onEachClass(root, PrivateDirectiveProcessor.class));
    }

    @Test
    void onEachClassNoNullaryDirectiveProcessor() {
        assertThatExceptionOfType(Bug.class)
            .isThrownBy(() -> new DirectivePlugin().onEachClass(root, NoNullaryDirectiveProcessor.class));
    }

    @Test
    void onEachClass() {
        new DirectivePlugin().onEachClass(root, TestDirectiveProcessor.class);

        assertThat(root.getSettings(DirectiveSettings.class).getProcessor("test"))
            .isInstanceOf(TestDirectiveProcessor.class);
    }

}
