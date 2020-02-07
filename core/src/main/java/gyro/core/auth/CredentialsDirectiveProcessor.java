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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Optional;

import com.google.common.base.CaseFormat;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("credentials")
public class CredentialsDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public void process(RootScope scope, DirectiveNode node) {
        validateArguments(node, 1, 2);

        String type = getArgument(scope, node, String.class, 0);
        String name = Optional.ofNullable(getArgument(scope, node, String.class, 1)).orElse("default");
        Scope bodyScope = evaluateBody(scope, node);

        CredentialsSettings settings = scope.getSettings(CredentialsSettings.class);
        Class<? extends Credentials> credentialsClass = settings.getCredentialsClasses().get(type);

        if (credentialsClass == null) {
            throw new GyroException(
                "Make sure @|magenta '@plugin'|@ directive for a cloud provider is placed before @|magenta '@credentials'|@ directive in @|magenta '.gyro/init.gyro'|@ file.");
        }
        Credentials credentials = Reflections.newInstance(credentialsClass);
        credentials.scope = scope;

        for (PropertyDescriptor property : Reflections.getBeanInfo(credentialsClass).getPropertyDescriptors()) {
            Method setter = property.getWriteMethod();

            if (setter != null) {
                Reflections.invoke(setter, credentials, scope.convertValue(
                    setter.getGenericParameterTypes()[0],
                    bodyScope.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName()))));
            }
        }

        credentials.getNamespaces().stream()
            .map(ns -> ns + "::" + name)
            .forEach(n -> settings.getCredentialsByName().put(n, credentials));
    }

}
