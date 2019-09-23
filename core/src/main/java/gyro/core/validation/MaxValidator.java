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

public class MaxValidator extends AbstractValidator<Max> {

    @Override
    protected boolean validate(Max annotation, Object value) {
        return value instanceof Number && ((Number) value).doubleValue() <= annotation.value();
    }

    @Override
    public String getMessage(Max annotation) {
        return String.format("Must be less than or equal to @|bold %s|@", annotation.value());
    }

}
