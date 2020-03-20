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
import gyro.lang.ast.OptionArgumentNode;
import gyro.lang.ast.value.Option;
import gyro.parser.antlr4.GyroParser;
import gyro.util.ImmutableCollectors;

public class DirectiveNode extends OptionArgumentNode {

    private final String name;
    private final List<DirectiveSection> sections;
    private final List<Node> body;

    public DirectiveNode(
        String name,
        List<Node> arguments,
        List<Option> options,
        List<Node> body,
        List<DirectiveSection> sections) {

        super(arguments, options);

        this.name = Preconditions.checkNotNull(name);
        this.sections = ImmutableList.copyOf(Preconditions.checkNotNull(sections));
        this.body = ImmutableList.copyOf(Preconditions.checkNotNull(body));
    }

    public DirectiveNode(GyroParser.DirectiveContext context) {
        super(Preconditions.checkNotNull(context));

        this.name = context.directiveType().getText();

        this.sections = context.section()
            .stream()
            .map(DirectiveSection::new)
            .collect(ImmutableCollectors.toList());

        this.body = Node.create(context.body());
    }

    public String getName() {
        return name;
    }

    public List<DirectiveSection> getSections() {
        return sections;
    }

    public List<Node> getBody() {
        return body;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitDirective(this, context);
    }
}
