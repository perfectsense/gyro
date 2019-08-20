package gyro.lang.ast.block;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.parser.antlr4.GyroParser;
import gyro.util.ImmutableCollectors;

import java.util.List;

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
