package gyro.lang.ast;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.value.Option;
import gyro.util.ImmutableCollectors;

public abstract class OptionArgumentNode extends Node {

    private final List<Node> arguments;
    private final List<Option> options;

    public OptionArgumentNode(List<Node> arguments, List<Option> options) {
        super(null);

        this.arguments = ImmutableList.copyOf(Preconditions.checkNotNull(arguments));
        this.options = ImmutableList.copyOf(Preconditions.checkNotNull(options));
    }

    public OptionArgumentNode(gyro.parser.antlr4.GyroParser.ReferenceContext context) {
        super(Preconditions.checkNotNull(context));

        this.arguments = Optional.ofNullable(context.IDENTIFIER())
            .map(Node::create)
            .map(Collections::singletonList)
            .orElseGet(() -> Node.create(context.value()));

        this.options = context.option()
            .stream()
            .map(Option::new)
            .collect(ImmutableCollectors.toList());
    }

    public OptionArgumentNode(gyro.parser.antlr4.GyroParser.DirectiveContext context) {
        super(Preconditions.checkNotNull(context));

        this.arguments = Node.create(context.arguments());

        this.options = context.option()
            .stream()
            .map(Option::new)
            .collect(ImmutableCollectors.toList());
    }

    public List<Node> getArguments() {
        return arguments;
    }

    public List<Option> getOptions() {
        return options;
    }
}
