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

package gyro.lang;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class SyntaxErrorException extends RuntimeException {

    private final String file;
    private final List<SyntaxError> errors;

    public SyntaxErrorException(String file, List<SyntaxError> errors) {
        this.file = Preconditions.checkNotNull(file);
        this.errors = ImmutableList.copyOf(Preconditions.checkNotNull(errors));
    }

    public String getFile() {
        return file;
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

}
