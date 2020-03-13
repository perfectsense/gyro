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

import java.util.Map;

import com.google.common.base.Preconditions;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.Resource;

public class FileScope extends Scope {

    private final String file;

    public FileScope(RootScope parent, String file) {
        super(parent);

        this.file = Preconditions.checkNotNull(file);
    }

    public static FileScope copy(RootScope parent, FileScope fileScope, boolean workflowResourceOnly) {
        FileScope scope = new FileScope(parent, fileScope.getFile());

        if (workflowResourceOnly) {
            for (Map.Entry<String, Object> entry : fileScope.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof Resource && DiffableInternals.getModifiedIn((Resource) value) != null) {
                    scope.put(entry.getKey(), value);
                }
            }
        } else {
            scope.putAll(fileScope);
        }
        return scope;
    }

    public String getFile() {
        return file;
    }

}
