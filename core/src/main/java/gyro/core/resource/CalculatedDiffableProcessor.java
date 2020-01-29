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

import java.util.Set;
import java.util.stream.Collectors;

public class CalculatedDiffableProcessor extends DiffableProcessor {

    @Override
    public Set<String> process(Diffable diffable) {
        DiffableType<Diffable> type = DiffableType.getInstance(diffable);

        return type.getFields()
            .stream()
            .filter(DiffableField::isCalculated)
            .map(DiffableField::getName)
            .collect(Collectors.toSet());
    }

}
