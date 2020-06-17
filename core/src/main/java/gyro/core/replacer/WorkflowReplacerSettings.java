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

package gyro.core.replacer;

import java.util.HashMap;
import java.util.Map;

import gyro.core.WorkflowReplacer;
import gyro.core.scope.Settings;

public class WorkflowReplacerSettings extends Settings {

    private Map<String, Class<? extends WorkflowReplacer>> workflowReplacerClasses;
    private WorkflowReplacer workflowReplacer;
    private Boolean skip;

    public Map<String, Class<? extends WorkflowReplacer>> getWorkflowReplacerClasses() {
        if (workflowReplacerClasses == null) {
            workflowReplacerClasses = new HashMap<>();
        }
        return workflowReplacerClasses;
    }

    public void setWorkflowReplacerClasses(Map<String, Class<? extends WorkflowReplacer>> workflowReplacerClasses) {
        this.workflowReplacerClasses = workflowReplacerClasses;
    }

    public WorkflowReplacer getWorkflowReplacer() {
        return workflowReplacer;
    }

    public void setWorkflowReplacer(WorkflowReplacer workflowReplacer) {
        this.workflowReplacer = workflowReplacer;
    }

    public boolean isSkip() {
        return Boolean.TRUE.equals(skip);
    }

    public void setSkip(Boolean skip) {
        this.skip = skip;
    }
}
