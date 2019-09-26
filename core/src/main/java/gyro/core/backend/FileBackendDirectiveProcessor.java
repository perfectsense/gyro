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

package gyro.core.backend;

import com.google.common.base.CaseFormat;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.FileBackend;
import gyro.core.Reflections;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

@Type("file-backend")
public class FileBackendDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public void process(RootScope scope, DirectiveNode node) throws Exception {
        validateArguments(node, 0, 2);
        String type = getArgument(scope, node, String.class, 0);
        String name = getArgument(scope, node, String.class, 1);

        Scope bodyScope = evaluateBody(scope, node);

        FileBackendsSettings settings = scope.getSettings(FileBackendsSettings.class);
        Class<? extends FileBackend> fileBackendClass = settings.getFileBackendsClasses().get(type);

        FileBackend fileBackend = Reflections.newInstance(fileBackendClass);
        fileBackend.setName(name);

        for (PropertyDescriptor property : Reflections.getBeanInfo(fileBackendClass).getPropertyDescriptors()) {

            Method setter = property.getWriteMethod();
            if (setter != null) {
                Reflections.invoke(setter, fileBackend, scope.convertValue(
                        setter.getGenericParameterTypes()[0],
                        bodyScope.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName()))));
            }
        }
        settings.getFileBackends().put(name, fileBackend);
    }

}
