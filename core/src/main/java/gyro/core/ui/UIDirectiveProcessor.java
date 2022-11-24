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

package gyro.core.ui;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

import com.google.common.base.CaseFormat;
import gyro.core.GyroCore;
import gyro.core.GyroException;
import gyro.core.GyroUI;
import gyro.core.Reflections;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("ui")
public class UIDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public void process(RootScope scope, DirectiveNode node) throws Exception {
        validateArguments(node, 1, 1);

        String type = getArgument(scope, node, String.class, 0);

        UISettings settings = scope.getSettings(UISettings.class);
        Class<? extends GyroUI> uiClass = settings.getUiClasses().get(type);

        if (uiClass == null) {
            throw new GyroException(
                "Make sure @|magenta '@plugin'|@ directive for a UI provider is placed before @|magenta '@ui'|@ directive in @|magenta '.gyro/ui.gyro'|@ file.");
        }

        GyroUI ui = Reflections.newInstance(uiClass);

        Scope bodyScope = evaluateBody(scope, node);

        for (PropertyDescriptor property : Reflections.getBeanInfo(uiClass).getPropertyDescriptors()) {
            Method setter = property.getWriteMethod();
            Object value = bodyScope.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName()));

            if (setter != null && value != null) {
                Reflections.invoke(setter, ui, scope.convertValue(setter.getGenericParameterTypes()[0], value));
            }
        }

        if (!uiClass.isInstance(GyroCore.ui())) {
            GyroCore.pushUi(ui);
        }
    }
}
