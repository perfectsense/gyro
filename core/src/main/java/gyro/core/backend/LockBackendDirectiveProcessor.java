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

package gyro.core.backend;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import com.google.common.base.CaseFormat;
import gyro.core.LockBackend;
import gyro.core.Reflections;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("lock-backend")
public class LockBackendDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public void process(RootScope scope, DirectiveNode node) throws Exception {
        validateArguments(node, 0, 1);
        String type = getArgument(scope, node, String.class, 0);

        LockBackendSettings settings = scope.getSettings(LockBackendSettings.class);
        if (settings.getLockBackend() != null) {
            return;
        }

        Scope bodyScope = evaluateBody(scope, node);

        Class<? extends LockBackend> lockBackendClass = settings.getLockBackendClasses().get(type);

        LockBackend lockBackend = Reflections.newInstance(lockBackendClass);

        for (PropertyDescriptor property : Reflections.getBeanInfo(lockBackendClass).getPropertyDescriptors()) {

            Method setter = property.getWriteMethod();
            if (setter != null) {
                Reflections.invoke(setter, lockBackend, scope.convertValue(
                    setter.getGenericParameterTypes()[0],
                    bodyScope.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName()))));
            }
        }

        lockBackend.setRootScope(scope);

        settings.setLockBackend(lockBackend);
    }

}
