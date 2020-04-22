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
import java.util.Optional;

import com.google.common.base.CaseFormat;
import gyro.core.FileBackend;
import gyro.core.GyroException;
import gyro.core.Reflections;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("state-backend")
public class StateBackendDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public void process(RootScope scope, DirectiveNode node) throws Exception {
        validateArguments(node, 1, 2);
        String type = getArgument(scope, node, String.class, 0);
        String name = Optional.ofNullable(getArgument(scope, node, String.class, 1)).orElse("default");

        if ("local".equals(name)) {
            throw new GyroException("'local' cannot be used as a 'state-backend' name!");
        }

        StateBackendSettings stateBackendSettings = scope.getSettings(StateBackendSettings.class);

        stateBackendSettings.getStateBackends().computeIfAbsent(name, n -> {
            Scope bodyScope = evaluateBody(scope, node);

            FileBackendsSettings fileBackendsSettings = scope.getSettings(FileBackendsSettings.class);
            Class<? extends FileBackend> fileBackendClass = fileBackendsSettings.getFileBackendsClasses().get(type);

            FileBackend fileBackend = Reflections.newInstance(fileBackendClass);

            for (PropertyDescriptor property : Reflections.getBeanInfo(fileBackendClass).getPropertyDescriptors()) {

                Method setter = property.getWriteMethod();
                if (setter != null) {
                    Reflections.invoke(setter, fileBackend, scope.convertValue(
                        setter.getGenericParameterTypes()[0],
                        bodyScope.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName()))));
                }
            }

            fileBackend.setName(n);
            fileBackend.setRootScope(scope);
            return fileBackend;
        });
    }

}
