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

import org.apache.commons.lang3.StringUtils;

public class RegexesValidator extends AbstractValidator<Regexes> {

    private static final RegexValidator VALIDATOR = new RegexValidator();

    @Override
    protected boolean validate(Regexes annotation, Object value) {
        return Stream.of(annotation.value()).anyMatch(a -> VALIDATOR.validate(a, value));
    }

    String getMessage(Regex... annotations) {
        return "Must match " + Stream.of(annotations)
            .map(a -> {
                String message = a.message();
                return StringUtils.isBlank(message)
                    ? String.format("@|bold %s|@", a.value())
                    : message;
            })
            .collect(Collectors.joining(", or "));
    }

    @Override
    public String getMessage(Regexes annotation) {
        return getMessage(annotation.value());
    }

}
