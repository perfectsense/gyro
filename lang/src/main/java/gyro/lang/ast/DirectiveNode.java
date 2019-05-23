package gyro.lang.ast;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.parser.antlr4.GyroParser;
import gyro.util.ImmutableCollectors;

import java.util.List;

public class DirectiveNode extends Node {

    private final String name;
    private final List<Node> arguments;

    public DirectiveNode(String name, List<Node> arguments) {
        this.name = Preconditions.checkNotNull(name);
        this.arguments = ImmutableList.copyOf(Preconditions.checkNotNull(arguments));
    }

    public DirectiveNode(GyroParser.DirectiveContext context) {
        this(
            Preconditions.checkNotNull(context).IDENTIFIER().getText(),
            context.directiveArgument()
                .stream()
                .map(n -> Node.create(n.getChild(0)))
                .collect(ImmutableCollectors.toList()));
    }

    public String getName() {
        return name;
    }

    public List<Node> getArguments() {
        return arguments;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitDirective(this, context);
    }

}
