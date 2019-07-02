package gyro.lang.ast.block;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class DirectiveSection {

    private final String name;
    private final List<Node> arguments;
    private final List<Node> body;

    public DirectiveSection(String name, List<Node> arguments, List<Node> body) {
        this.name = Preconditions.checkNotNull(name);
        this.arguments = ImmutableList.copyOf(Preconditions.checkNotNull(arguments));
        this.body = ImmutableList.copyOf(Preconditions.checkNotNull(body));
    }

    public DirectiveSection(GyroParser.SectionContext context) {
        GyroParser.OptionContext optionContext = Preconditions.checkNotNull(context).option();

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
