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

import java.util.List;

import com.google.common.collect.ImmutableList;
import gyro.core.GyroUI;

public class ValidationErrorException extends RuntimeException {

    private final List<ValidationError> errors;

    public ValidationErrorException(List<ValidationError> errors) {
        this.errors = ImmutableList.copyOf(errors);
    }

    public void write(GyroUI ui) {
        ui.write("@|red %d validation errors!|@\n", errors.size());

        for (ValidationError e : errors) {
            e.write(ui);
        }
    }

}
