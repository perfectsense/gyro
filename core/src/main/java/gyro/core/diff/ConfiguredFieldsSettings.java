package gyro.core.diff;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gyro.core.scope.Settings;

public class ConfiguredFieldsSettings extends Settings {

    Map<String, Map<String, Set<String>>> storedConfiguredFields = new HashMap<>();

    public Map<String, Map<String, Set<String>>> getStoredConfiguredFields() {
        return storedConfiguredFields;
    }

    public void setStoredConfiguredFields(Map<String, Map<String, Set<String>>> storedConfiguredFields) {
        this.storedConfiguredFields = storedConfiguredFields;
    }

}
