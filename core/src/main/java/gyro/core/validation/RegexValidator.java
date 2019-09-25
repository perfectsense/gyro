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

public class RegexValidator extends AbstractValidator<Regex> {

    private static final RegexesValidator VALIDATOR = new RegexesValidator();

    @Override
    protected boolean validate(Regex annotation, Object value) {
        return value instanceof String && ((String) value).matches(annotation.value());
    }

    @Override
    public String getMessage(Regex annotation) {
        return VALIDATOR.getMessage(annotation);
    }

}
