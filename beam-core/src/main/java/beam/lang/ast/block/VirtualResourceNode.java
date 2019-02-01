package beam.lang.ast.block;

import beam.lang.BeamLanguageException;
import beam.lang.ast.Node;
import beam.lang.ast.scope.Scope;

import java.util.List;
import java.util.stream.Collectors;

import static beam.parser.antlr4.BeamParser.VirtualResourceContext;

public class VirtualResourceNode extends BlockNode {

    private List<Node> parameters;

    public VirtualResourceNode(VirtualResourceContext context) {
        super(context.virtualResourceBody()
                .stream()
                .map(b -> Node.create(b))
                .collect(Collectors.toList()));

        parameters = context.virtualResourceParam()
            .stream()
            .map(p -> Node.create(p))
            .collect(Collectors.toList());
    }

    @Override
    public Object evaluate(Scope scope) throws Exception {
        Scope bodyScope = new Scope(null);

        for (Node parameterNode : parameters) {
            String parameter = ((VirtualResourceParamNode) parameterNode).getName();

            if (!scope.containsKey(parameter)) {
                throw new BeamLanguageException(String.format("Required parameter '%s' is missing.", parameter));
            }

            bodyScope.put(parameter, scope.get(parameter));
        }

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {

    }

}
