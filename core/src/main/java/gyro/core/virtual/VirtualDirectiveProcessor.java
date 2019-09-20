package gyro.core.virtual;

import java.util.List;

import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.FileScope;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.RootScope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.block.FileNode;
import gyro.lang.ast.block.ResourceNode;

@Type("virtual")
public class VirtualDirectiveProcessor extends DirectiveProcessor<FileScope> {

    @Override
    public void process(FileScope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);

        RootScope root = scope.getRootScope();
        String type = getArgument(scope, node, String.class, 0);
        List<Node> body = node.getBody();

        root.put(type, new VirtualResourceVisitor(scope, body));

        // When virtual directive defines other resources, wildcard queries to those types of resources should defer
        // until all virtual resources are actually created.
        NodeEvaluator evaluator = root.getEvaluator();

        evaluator.getBody()
            .stream()
            .filter(FileNode.class::isInstance)
            .map(FileNode.class::cast)
            .map(FileNode::getBody)
            .flatMap(List::stream)
            .filter(ResourceNode.class::isInstance)
            .map(ResourceNode.class::cast)
            .filter(r -> type.equals(r.getType()))
            .forEach(vr -> body.forEach(i -> evaluator.addTypeNode(vr, i)));
    }

}
