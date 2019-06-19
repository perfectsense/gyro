package gyro.lang.ast.block;

import java.util.List;
import java.util.stream.Collectors;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class VirtualResourceNode extends BlockNode {

    private String name;
    private List<VirtualResourceParameter> parameters;

    public VirtualResourceNode(GyroParser.VirtualResourceContext context) {
        super(context.blockBody()
                .blockStatement()
                .stream()
                .map(b -> Node.create(b.getChild(0)))
                .collect(Collectors.toList()));

        name = context.type().getText();

        parameters = context.virtualResourceParameter()
                .stream()
                .map(VirtualResourceParameter::new)
                .collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public List<VirtualResourceParameter> getParameters() {
        return parameters;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitVirtualResource(this, context);
    }

}
