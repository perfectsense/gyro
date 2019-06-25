package gyro.lang.ast.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DirectiveNode extends BlockNode {

    private final String name;
    private final List<Node> arguments;

    public DirectiveNode(String name, List<Node> arguments, List<Node> body) {
        super(body);

        this.name = Preconditions.checkNotNull(name);
        this.arguments = ImmutableList.copyOf(Preconditions.checkNotNull(arguments));
    }

    public DirectiveNode(GyroParser.DirectiveContext context) {
        super(Optional.ofNullable(Preconditions.checkNotNull(context).blockBody())
            .map(GyroParser.BlockBodyContext::blockStatement)
            .map(Node::create)
            .orElseGet( Collections::emptyList));

        this.name = Preconditions.checkNotNull(context).IDENTIFIER().getText();
        this.arguments = Node.create(context.value());
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
