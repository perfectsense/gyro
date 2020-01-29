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

import java.util.concurrent.TimeUnit;

public class Wait {

    public static Waiter atMost(long duration, TimeUnit unit) {
        return new Waiter().atMost(duration, unit);
    }

    public static Waiter checkEvery(long duration, TimeUnit unit) {
        return new Waiter().checkEvery(duration, unit);
    }

    public static Waiter prompt(boolean prompt) {
        return new Waiter().prompt(prompt);
    }

    public static boolean until(WaitCheck check) {
        return new Waiter().until(check);
    }
}
