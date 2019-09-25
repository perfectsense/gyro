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

package gyro.core;

import gyro.lang.Locatable;

public class GyroException extends RuntimeException {

    private final Locatable locatable;

    public GyroException(Locatable locatable, String message, Throwable cause) {
        super(message, cause);
        this.locatable = locatable;
    }

    public GyroException(Locatable locatable, String message) {
        this(locatable, message, null);
    }

    public GyroException(Locatable locatable, Throwable cause) {
        this(locatable, null, cause);
    }

    public GyroException(String message, Throwable cause) {
        this(null, message, cause);
    }

    public GyroException(String message) {
        this(null, message, null);
    }

    public GyroException(Throwable cause) {
        this(null, null, cause);
    }

    public Locatable getLocatable() {
        return locatable;
    }

}
