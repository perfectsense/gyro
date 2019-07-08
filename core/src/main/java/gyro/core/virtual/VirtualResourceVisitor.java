package gyro.core.virtual;

import java.util.List;

import com.google.common.collect.ImmutableList;
import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.DeferError;
import gyro.core.resource.DiffableInternals;
import gyro.core.resource.FileScope;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Resource;
import gyro.core.resource.ResourceVisitor;
import gyro.core.resource.RootScope;
import gyro.core.resource.Scope;
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
                    List<Object> arguments = DirectiveProcessor.evaluateArguments(scope, directive);

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
        virtualRoot.getEvaluator().visitBody(body, virtualFile);

        String prefix = name + "/";

        for (Resource resource : virtualRoot.findResources()) {
            DiffableInternals.setName(resource, prefix + resource.name());
            root.addResource(resource);
        }
    }

}
