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

package gyro.core.validation;

import java.util.Set;
import java.util.stream.Stream;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.DiffableType;

public class ConflictsWithValidator implements Validator<ConflictsWith> {

    @Override
    public boolean isValid(Diffable diffable, ConflictsWith annotation, Object fieldValue) {
        if (ObjectUtils.isBlank(fieldValue)) {
            return true;
        }
        Set<String> configuredFields = DiffableInternals.getConfiguredFields(diffable);
        DiffableType<Diffable> diffableType = DiffableType.getInstance(diffable);

        return Stream.of(annotation.value())
            .filter(name -> configuredFields.contains(name))
            .allMatch(name -> {
                DiffableField field = diffableType.getField(name);
                return field == null || ObjectUtils.isBlank(field.getValue(diffable));
            });
    }

    @Override
    public String getMessage(ConflictsWith annotation) {
        return String.format(
            "Cannot be set when any of the following field(s) are set: ['%s']",
            String.join("', '", annotation.value()));
    }
}
