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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Waiter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Waiter.class);

    private long atMost;
    private long checkEvery;
    private boolean prompt;

    public Waiter() {
        atMost(10, TimeUnit.SECONDS);
        checkEvery(1, TimeUnit.SECONDS);
        prompt(true);
    }

    public Waiter atMost(long duration, TimeUnit unit) {
        this.atMost = unit.toMillis(duration);
        return this;
    }

    public Waiter checkEvery(long duration, TimeUnit unit) {
        this.checkEvery = unit.toMillis(duration);
        return this;
    }

    public Waiter prompt(boolean prompt) {
        this.prompt = prompt;
        return this;
    }

    public boolean until(WaitCheck check) {
        do {
            long startTime = System.currentTimeMillis();

            while (true) {
                try {
                    if (check.check()) {
                        return true;
                    }

                } catch (Exception error) {
                    throw new GyroException("Failed wait check!", error);
                }

                try {
                    Thread.sleep(checkEvery);

                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    break;
                }

                if (atMost < System.currentTimeMillis() - startTime) {
                    break;
                }
            }
        } while (prompt && GyroCore.ui().readBoolean(Boolean.TRUE, "\nWait for completion?"));

        return false;
    }
}
