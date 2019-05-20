package gyro.lang.ast;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.value.ValueNode;
import gyro.parser.antlr4.GyroParser;
import gyro.util.ImmutableCollectors;

import java.util.List;

public class DirectiveNode extends Node {

    private final String name;
    private final List<ValueNode> arguments;

    public DirectiveNode(String name, List<ValueNode> arguments) {
        this.name = Preconditions.checkNotNull(name);
        this.arguments = ImmutableList.copyOf(Preconditions.checkNotNull(arguments));
    }

    public DirectiveNode(GyroParser.DirectiveContext context) {
        this(
            Preconditions.checkNotNull(context).IDENTIFIER().getText(),
            context.value()
                .stream()
                .map(Node::create)
                .map(ValueNode.class::cast)
                .collect(ImmutableCollectors.toList()));
    }

    public String getName() {
        return name;
    }

    public List<ValueNode> getArguments() {
        return arguments;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitDirective(this, context);
    }

}
