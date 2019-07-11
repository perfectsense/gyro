package gyro.lang.ast.block;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import gyro.lang.ast.Rule;
import gyro.parser.antlr4.GyroParser;

public class DirectiveOption extends Rule {

    private final String name;
    private final List<Node> arguments;

    public DirectiveOption(String name, List<Node> arguments) {
        super(null);

        this.name = Preconditions.checkNotNull(name);
        this.arguments = ImmutableList.copyOf(Preconditions.checkNotNull(arguments));
    }

    public DirectiveOption(GyroParser.OptionContext context) {
        super(Preconditions.checkNotNull(context));

        this.name = context.IDENTIFIER().getText();
        this.arguments = Node.create(context.arguments());
    }

    public String getName() {
        return name;
    }

    public List<Node> getArguments() {
        return arguments;
    }

}
