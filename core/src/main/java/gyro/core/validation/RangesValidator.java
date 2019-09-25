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

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RangesValidator extends AbstractValidator<Ranges> {

    private static final RangeValidator VALIDATOR = new RangeValidator();

    @Override
    protected boolean validate(Ranges annotation, Object value) {
        return Stream.of(annotation.value()).anyMatch(a -> VALIDATOR.validate(a, value));
    }

    String getMessage(Range... annotations) {
        return "Must be between " + Stream.of(annotations)
            .map(a -> String.format("@|bold %s|@ and @|bold %s|@", a.min(), a.max()))
            .collect(Collectors.joining(", or "));
    }

    @Override
    public String getMessage(Ranges annotation) {
        return getMessage(annotation.value());
    }

}