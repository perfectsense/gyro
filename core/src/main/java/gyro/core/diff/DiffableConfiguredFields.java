/*
 * Copyright 2020, Perfect Sense, Inc.
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

package gyro.core.diff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

class DiffableConfiguredFields {

    /**
     * Primary key of a {@link gyro.core.resource.Diffable} object.
     */
    private String primaryKey;

    /**
     * Configured fields of a {@link gyro.core.resource.Diffable} object.
     */
    private Set<String> configuredFields;

    /**
     * Map of field name to {@link DiffableConfiguredFields}.
     */
    private Map<String, List<DiffableConfiguredFields>> children;

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public Set<String> getConfiguredFields() {
        return configuredFields;
    }

    public void setConfiguredFields(Set<String> configuredFields) {
        this.configuredFields = configuredFields;
    }

    public Map<String, List<DiffableConfiguredFields>> getChildren() {
        if (children == null) {
            children = new HashMap();
        }
        return children;
    }

    public void setChildren(Map<String, List<DiffableConfiguredFields>> children) {
        this.children = children;
    }
}
