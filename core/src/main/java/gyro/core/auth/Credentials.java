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

package gyro.core.auth;

import java.nio.file.Paths;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import gyro.core.GyroException;
import gyro.core.GyroInputStream;
import gyro.core.Reflections;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.FileScope;
import gyro.core.scope.Scope;

public abstract class Credentials {

    Scope scope;

    @SuppressWarnings("unchecked")
    public static <C extends Credentials> C getInstance(Class<C> credentialsClass, Class<?> contextClass, Scope scope) {
        DiffableScope diffableScope = scope.getClosest(DiffableScope.class);

        String name = diffableScope != null
            ? diffableScope.getSettings(CredentialsSettings.class).getUseCredentials()
            : null;

        name = Reflections.getNamespace(contextClass) + "::" + (name != null ? name : "default");

        Credentials credentials = scope.getRootScope()
            .getSettings(CredentialsSettings.class)
            .getCredentialsByName()
            .get(name);

        if (credentials == null) {
            throw new GyroException(String.format(
                "Can't find @|bold %s|@ credentials!",
                name));
        }

        if (!credentialsClass.isInstance(credentials)) {
            throw new GyroException(String.format(
                "Can't use @|bold %s|@ credentials because it's an instance of @|bold %s|@, not @|bold %s|@!",
                name,
                credentials.getClass().getName(),
                credentialsClass.getName()));
        }

        return (C) credentials;
    }

    public Set<String> getNamespaces() {
        return ImmutableSet.of(Reflections.getNamespace(getClass()));
    }

    public void refresh() {
    }

    public GyroInputStream openInput(String file) {
        FileScope fileScope = scope.getFileScope();

        return fileScope.getRootScope()
            .openInput(Paths.get(fileScope.getFile())
                .getParent()
                .resolve(file)
                .toString());
    }

}
