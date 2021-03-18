/*
 * Copyright 2021, Brightspot.
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

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import gyro.core.scope.Settings;

public class TimeoutSettings extends Settings {

    private final Map<Action, Timeout> timeouts = new EnumMap<>(Action.class);

    public Map<Action, Timeout> getTimeouts() {
        return timeouts;
    }

    public static class Timeout {

        private Long atMostDuration;
        private Long checkEveryDuration;
        private Boolean prompt;
        private Boolean skip;

        public void setAtMost(String duration) {
            if (duration != null) {
                atMostDuration = parse(duration);
            }
        }

        public Long getAtMostDuration() {
            return atMostDuration;
        }

        public void setCheckEvery(String duration) {
            if (duration != null) {
                checkEveryDuration = parse(duration);
            }
        }

        public Long getCheckEveryDuration() {
            return checkEveryDuration;
        }

        public Boolean isPrompt() {
            return prompt;
        }

        public void setPrompt(Boolean prompt) {
            this.prompt = prompt;
        }

        public Boolean isSkip() {
            return skip;
        }

        public void setSkip(Boolean skip) {
            this.skip = skip;
        }

        private long parse(String duration) {
            Preconditions.checkNotNull(duration);

            try {
                duration = (duration.startsWith("PT") ? duration : "PT" + duration).toUpperCase();

                return Duration.parse(duration).getSeconds();
            } catch (DateTimeParseException ex) {
                throw new GyroException("Time format '" + duration + "' is invalid.");
            }
        }

    }

    public static enum Action {
        CREATE, UPDATE, DELETE;
    }
}
