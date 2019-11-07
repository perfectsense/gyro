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

import com.psddev.dari.util.ObjectUtils;
import gyro.core.scope.RootScope;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.stream.Stream;

public abstract class FileBackend {

    private String name;
    private RootScope rootScope;
    private String credentials;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RootScope getRootScope() {
        return rootScope;
    }

    public void setRootScope(RootScope rootScope) {
        this.rootScope = rootScope;
    }

    public String getCredentials() {
        if (ObjectUtils.isBlank(credentials)) {
            setCredentials("default");
        }

        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }


    public abstract Stream<String> list() throws Exception;

    public abstract InputStream openInput(String file) throws Exception;

    public abstract OutputStream openOutput(String file) throws Exception;

    public abstract void delete(String file) throws Exception;

}
