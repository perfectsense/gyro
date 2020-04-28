/*
 * Copyright 2020, Perfect Sense, Inc.
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

import gyro.core.scope.RootScope;

public abstract class LockBackend {

    public static final String TEMP_LOCK_ID_FILE = "TempLockId.txt";

    private RootScope rootScope;
    private String lockId;
    private Boolean stayLocked;

    public RootScope getRootScope() {
        return rootScope;
    }

    public void setRootScope(RootScope rootScope) {
        this.rootScope = rootScope;
    }

    public String getLockId() {
        return lockId;
    }

    public void setLockId(String lockId) {
        this.lockId = lockId;
    }

    public boolean stayLocked() {
        return Boolean.TRUE.equals(stayLocked);
    }

    public void setStayLocked(Boolean stayLocked) {
        this.stayLocked = stayLocked;
    }

    /**
     * Attempts to lock the state. Throw an exception in the case of already locked. The exception message should contain the new lock ID so that users can forcefully unlock if they want.
     */
    public abstract void lock(String lockId) throws Exception;

    /**
     * Attempts to unlock the state. Throw an exception in the case of {@param lockId} no longer being the active lock.
     */
    public abstract void unlock(String lockId) throws Exception;

    /**
     * Sets the lock info on the {@param lockId}. Throw an exception in the case of {@param lockId} no longer being the active lock.
     */
    public abstract void updateLockInfo(String lockId, String info) throws Exception;

    public void lock() throws Exception {
        lock(getLockId());
    }

    public void unlock() throws Exception {
        unlock(getLockId());
    }

    public void updateLockInfo(String info) throws Exception {
        updateLockInfo(getLockId(), info);
    }
}
