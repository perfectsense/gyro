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

class CreateDefer extends Defer {

    private final String key;

    public CreateDefer(Defer cause, String type, String name) {
        super(
            null,
            String.format("Can't create @|bold %s|@ @|bold %s|@ resource!", type, name),
            cause);

        this.key = type + "::" + name;
    }

    public String getKey() {
        return key;
    }

}
