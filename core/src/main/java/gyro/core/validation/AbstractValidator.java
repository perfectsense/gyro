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

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gyro.core.resource.Diffable;

public abstract class AbstractValidator<A extends Annotation> implements Validator<A> {

    protected abstract boolean validate(A annotation, Object value);

    @Override
    public final boolean isValid(Diffable diffable, A annotation, Object value) {
        if (value == null) {
            return true;

        } else if (value instanceof List) {
            return ((List<?>) value).stream().allMatch(o -> isValid(diffable, annotation, o));

        } else if (value instanceof Set) {
            return ((Set<?>) value).stream().allMatch(o -> isValid(diffable, annotation, o));

        } else if (value instanceof Map) {
            return ((Map<?, ?>) value).keySet().stream().allMatch(o -> isValid(diffable, annotation, o));

        } else {
            return value.toString().equals("<output>") || validate(annotation, value);
        }
    }

}
