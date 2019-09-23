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

import java.util.HashMap;
import java.util.Map;

import gyro.core.Reflections;
import gyro.core.scope.Scope;
import gyro.core.scope.Settings;

public class DirectiveSettings extends Settings {

    private final Map<String, DirectiveProcessor<? extends Scope>> processors = new HashMap<>();

    public DirectiveProcessor<? extends Scope> getProcessor(String type) {
        return processors.get(type);
    }

    public void addProcessor(Class<? extends DirectiveProcessor<? extends Scope>> processorClass) {
        String type = Reflections.getType(processorClass);

        processors.put(
            Reflections.getNamespaceOptional(processorClass)
                .map(ns -> ns + "::" + type)
                .orElse(type),
            Reflections.newInstance(processorClass));
    }

}
