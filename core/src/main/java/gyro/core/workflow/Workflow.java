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

package gyro.core.workflow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.psddev.dari.util.IoUtils;
import com.psddev.dari.util.ObjectUtils;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.GyroUI;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.core.scope.State;

public abstract class Workflow {

    public static final String EXECUTION_FILE = "workflow-execution.json";

    private final String type;
    private final String name;
    private final RootScope root;

    public Workflow(String type, String name, Scope scope) {
        this.type = Preconditions.checkNotNull(type);
        this.name = Preconditions.checkNotNull(name);
        this.root = Preconditions.checkNotNull(scope).getRootScope();
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public RootScope getRoot() {
        return root;
    }

    public abstract void execute(GyroUI ui, State state, Resource currentDiffable, Resource pendingDiffable);

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getExecution(RootScope root) {
        try (GyroInputStream input = root.openInput(Workflow.EXECUTION_FILE)) {
            return (Map<String, Object>) ObjectUtils.fromJson(IoUtils.toString(input, StandardCharsets.UTF_8));

        } catch (GyroException error) {
            return null;

        } catch (IOException error) {
            throw new GyroException(error);
        }
    }
}
