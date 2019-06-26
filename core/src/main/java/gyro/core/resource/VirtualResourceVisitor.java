package gyro.core.resource;

import java.util.List;

import com.google.common.collect.ImmutableList;
import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;

public class VirtualResourceVisitor extends ResourceVisitor {

    private final List<VirtualParameter> parameters;
    private final List<Node> body;

    public VirtualResourceVisitor(Scope scope, List<Node> body) {
        ImmutableList.Builder<VirtualParameter> parametersBuilder = ImmutableList.builder();
        ImmutableList.Builder<Node> bodyBuilder = ImmutableList.builder();

        for (Node child : body) {
            if (child instanceof DirectiveNode) {
                DirectiveNode directive = (DirectiveNode) child;

                if ("param".equals(directive.getName())) {
                    List<Object> arguments = DirectiveProcessor.resolveArguments(scope, directive);

                    if (arguments.size() != 1) {
                        throw new GyroException("@param directive only takes 1 argument!");
                    }

                    parametersBuilder.add(new VirtualParameter((String) arguments.get(0)));
                    continue;
                }
            }

            bodyBuilder.add(child);
        }

        this.parameters = parametersBuilder.build();
        this.body = bodyBuilder.build();
    }

    public List<VirtualParameter> getParameters() {
        return parameters;
    }

    public List<Node> getBody() {
        return body;
    }

    @Override
    public void visit(String name, Scope scope) {
        RootScope root = scope.getRootScope();
        RootScope virtualRoot = new RootScope(root.getFile(), root.getBackend(), null, root.getLoadFiles());

        virtualRoot.putAll(root);

        FileScope file = scope.getFileScope();
        FileScope virtualFile = new FileScope(virtualRoot, file.getFile());

        virtualRoot.getFileScopes().add(virtualFile);
        parameters.forEach(p -> p.copy(scope, virtualFile));

        NodeEvaluator evaluator = virtualRoot.getEvaluator();

        for (Node child : body) {
            evaluator.visit(child, virtualFile);
        }

        String prefix = name + "/";

        for (Resource resource : virtualRoot.findResources()) {
            resource.name = prefix + resource.name;
            file.put(resource.primaryKey(), resource);
        }
    }

}
