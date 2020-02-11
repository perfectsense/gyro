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

package gyro.core.metadata;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("metadata")
public class MetadataDirectiveProcessor extends DirectiveProcessor<Scope> {

    private static final Map<String, Object> METADATA = new ConcurrentHashMap<>();

    public static void putMetadata(String key, Object value) {
        METADATA.put(key, value);
    }

    public static Object removeMetadata(String key) {
        return METADATA.remove(key);
    }

    public static Object getMetadata(String key) {
        return METADATA.get(key);
    }

    public static Map<String, Object> getMetadataMap() {
        return METADATA;
    }

    @Override
    public void process(Scope scope, DirectiveNode node) throws Exception {
        validateArguments(node, 1, 2);

        String key = getArgument(scope, node, String.class, 0);
        Object value = getArgument(scope, node, Object.class, 1);

        if (value == null) {
            removeMetadata(key);
        } else {
            putMetadata(key, value);
        }
    }
}
