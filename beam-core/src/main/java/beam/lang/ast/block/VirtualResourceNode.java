package beam.lang.ast.block;

import beam.lang.BeamLanguageException;
import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;

import java.util.List;
import java.util.stream.Collectors;

import static beam.parser.antlr4.BeamParser.VirtualResourceContext;

public class VirtualResourceNode extends BlockNode {

    private String name;
    private List<Node> parameters;

    public VirtualResourceNode(VirtualResourceContext context) {
        super(context.virtualResourceBody()
                .stream()
                .map(b -> Node.create(b.getChild(0)))
                .collect(Collectors.toList()));

        name = context.virtualResourceName().IDENTIFIER().getText();

        parameters = context.virtualResourceParam()
            .stream()
            .map(p -> Node.create(p))
            .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        for (Node parameterNode : parameters) {
            String parameter = ((VirtualResourceParamNode) parameterNode).getName();

            if (!scope.containsKey(parameter)) {
                throw new BeamLanguageException(String.format("Required parameter '%s' is missing.", parameter));
            }
        }

        for (Node node : body) {
            node.evaluate(scope);
        }

        return null;
    }

    public String getName() {
        return name;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {

    }

}
