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

package gyro.lang.ast.value;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.filter.Filter;
import gyro.parser.antlr4.GyroParser;
import gyro.util.ImmutableCollectors;

public class ReferenceNode extends Node {

    private final List<Node> arguments;
    private final List<Filter> filters;
    private final List<ReferenceOption> options;

    public ReferenceNode(List<Node> arguments, Collection<Filter> filters, List<ReferenceOption> options) {
        super(null);

        this.arguments = ImmutableList.copyOf(Preconditions.checkNotNull(arguments));
        this.filters = ImmutableList.copyOf(Preconditions.checkNotNull(filters));
        this.options = ImmutableList.copyOf(Preconditions.checkNotNull(options));
    }

    public ReferenceNode(GyroParser.ReferenceContext context) {
        super(Preconditions.checkNotNull(context));

        this.arguments = Optional.ofNullable(context.IDENTIFIER())
            .map(Node::create)
            .map(Collections::singletonList)
            .orElseGet(() -> Node.create(context.value()));

        this.filters = context.filter()
            .stream()
            .map(Filter::create)
            .collect(ImmutableCollectors.toList());

        this.options = context.option()
            .stream()
            .map(ReferenceOption::new)
            .collect(ImmutableCollectors.toList());
    }

    public List<Node> getArguments() {
        return arguments;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public List<ReferenceOption> getOptions() {
        return options;
    }

    @Override
    public <C, R, X extends Throwable> R accept(NodeVisitor<C, R, X> visitor, C context) throws X {
        return visitor.visitReference(this, context);
    }

}
