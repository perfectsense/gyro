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
import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;
import gyro.util.ImmutableCollectors;

public class DirectiveNode extends BlockNode {

    private final String name;
    private final List<Node> arguments;
    private final List<DirectiveOption> options;
    private final List<DirectiveSection> sections;

    public DirectiveNode(
        String name,
        List<Node> arguments,
        List<DirectiveOption> options,
        List<Node> body,
        List<DirectiveSection> sections) {

        super(null, body);

        this.name = Preconditions.checkNotNull(name);
        this.arguments = ImmutableList.copyOf(Preconditions.checkNotNull(arguments));
        this.options = ImmutableList.copyOf(Preconditions.checkNotNull(options));
        this.sections = ImmutableList.copyOf(Preconditions.checkNotNull(sections));
    }

    public DirectiveNode(GyroParser.DirectiveContext context) {
        super(Preconditions.checkNotNull(context), Node.create(context.body()));

        this.name = context.directiveType().getText();
        this.arguments = Node.create(context.arguments());

        this.options = context.option()
            .stream()
            .map(DirectiveOption::new)
            .collect(ImmutableCollectors.toList());

        this.sections = context.section()
            .stream()
            .map(DirectiveSection::new)
            .collect(ImmutableCollectors.toList());
    }

    public String getName() {
        return name;
    }

    public List<Node> getArguments() {
        return arguments;
    }

    public List<DirectiveOption> getOptions() {
        return options;
    }

    public List<DirectiveSection> getSections() {
        return sections;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitDirective(this, context);
    }

}
