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

package gyro.lang.ast.block;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import gyro.lang.ast.Rule;
import gyro.parser.antlr4.GyroParser;

public class DirectiveSection extends Rule {

    private final String name;
    private final List<Node> arguments;
    private final List<Node> body;

    public DirectiveSection(String name, List<Node> arguments, List<Node> body) {
        super(null);

        this.name = Preconditions.checkNotNull(name);
        this.arguments = ImmutableList.copyOf(Preconditions.checkNotNull(arguments));
        this.body = ImmutableList.copyOf(Preconditions.checkNotNull(body));
    }

    public DirectiveSection(GyroParser.SectionContext context) {
        super(Preconditions.checkNotNull(context));

        GyroParser.OptionContext optionContext = context.option();

        this.name = optionContext.IDENTIFIER().getText();
        this.arguments = Node.create(optionContext.arguments());
        this.body = Node.create(context.body());
    }

    public String getName() {
        return name;
    }

    public List<Node> getArguments() {
        return arguments;
    }

    public List<Node> getBody() {
        return body;
    }

}
