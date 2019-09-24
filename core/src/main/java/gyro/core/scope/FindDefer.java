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

import gyro.lang.ast.Node;

class FindDefer extends Defer {

    private final String key;

    public FindDefer(Node node, String type, String name) {
        super(node, String.format("Can't find @|bold %s|@ resource of @|bold %s|@ type!", name, type));

        this.key = type + "::" + name;
    }

    public String getKey() {
        return key;
    }

}
