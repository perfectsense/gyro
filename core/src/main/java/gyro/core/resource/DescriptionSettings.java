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

package gyro.core.resource;

import java.util.HashMap;
import java.util.Map;

import gyro.core.scope.Settings;
import gyro.lang.ast.Node;

public class DescriptionSettings extends Settings {

    private Map<String, Node> typeDescriptions;
    private Node description;

    public Map<String, Node> getTypeDescriptions() {
        if (typeDescriptions == null) {
            typeDescriptions = new HashMap<>();
        }

        return typeDescriptions;
    }

    public void setTypeDescriptions(Map<String, Node> typeDescriptions) {
        this.typeDescriptions = typeDescriptions;
    }

    public Node getDescription() {
        return description;
    }

    public void setDescription(Node description) {
        this.description = description;
    }

}
