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

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ValidStringsValidator extends AbstractValidator<ValidStrings> {

    @Override
    protected boolean validate(ValidStrings annotation, Object value) {
        return value instanceof String && Arrays.asList(annotation.value()).contains(value);
    }

    @Override
    public String getMessage(ValidStrings annotation) {
        return "Must be one of " + Stream.of(annotation.value())
            .map(v -> String.format("@|bold %s|@", v))
            .collect(Collectors.joining(", "));
    }

}
