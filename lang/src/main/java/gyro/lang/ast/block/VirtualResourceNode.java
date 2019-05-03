package gyro.lang.ast.block;

import java.util.List;
import java.util.stream.Collectors;

import gyro.lang.ast.NodeVisitor;
import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class VirtualResourceNode extends BlockNode {

    private String name;
    private List<VirtualResourceParameter> params;

    public VirtualResourceNode(GyroParser.VirtualResourceContext context) {
        super(context.blockBody()
                .blockStatement()
                .stream()
                .map(b -> Node.create(b.getChild(0)))
                .collect(Collectors.toList()));

        name = context.resourceType().getText();

        params = context.virtualResourceParameter()
                .stream()
                .map(VirtualResourceParameter::new)
                .collect(Collectors.toList());
    }

    public String getName() {
        return name;
    }

    public List<VirtualResourceParameter> getParams() {
        return params;
    }

    @Override
    public <C> Object accept(NodeVisitor<C> visitor, C context) {
        return visitor.visitVirtualResource(this, context);
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {

    }

}
