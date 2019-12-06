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

import java.util.List;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("print")
public class PrintDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public void process(Scope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);
        Object argument = getArgument(scope, node, Object.class, 0);

        StringBuilder sb = new StringBuilder();
        if (argument instanceof List) {
            for (Object element : (List) argument) {
                sb.append(ObjectUtils.toJson(element, true));
                sb.append("\n");
            }
        } else {
            sb.append(ObjectUtils.toJson(argument, true));
            sb.append("\n");
        }

        print(sb.toString());
    }

    protected void print(String content) {
        GyroCore.ui().write(content);
        GyroCore.ui().write("\n");
    }

}
