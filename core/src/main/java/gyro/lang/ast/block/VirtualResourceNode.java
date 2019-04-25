package gyro.lang.ast.block;

import java.util.List;
import java.util.stream.Collectors;

import gyro.core.Credentials;
import gyro.core.GyroCore;
import gyro.core.resource.Resource;
import gyro.core.scope.FileScope;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.GyroLanguageException;
import gyro.lang.ast.Node;

import static gyro.parser.antlr4.GyroParser.VirtualResourceContext;

public class VirtualResourceNode extends BlockNode {

    private String name;
    private List<VirtualResourceParameter> params;

    public VirtualResourceNode(VirtualResourceContext context) {
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

    public void createResources(String prefix, Scope paramScope) throws Exception {
        FileScope paramFileScope = paramScope.getFileScope();
        RootScope vrScope = new RootScope(GyroCore.getRootInitFile().toString());
        FileScope resourceScope = new FileScope(vrScope, paramFileScope.getFile());

        for (VirtualResourceParameter param : params) {
            String paramName = param.getName();

            if (!paramScope.containsKey(paramName)) {
                throw new GyroLanguageException(String.format("Required parameter '%s' is missing.", paramName));

            } else {
                vrScope.put(paramName, paramScope.get(paramName));
            }
        }

        RootScope paramRootScope = paramScope.getRootScope();

        vrScope.getResourceClasses().putAll(paramRootScope.getResourceClasses());
        vrScope.getFileScopes().add(resourceScope);

        paramRootScope.findAllResources()
                .stream()
                .filter(Credentials.class::isInstance)
                .forEach(c -> vrScope.put(c.resourceType() + "::" + c.resourceIdentifier(), c));

        for (Node node : body) {
            node.evaluate(resourceScope);
        }

        for (Resource resource : vrScope.findAllResources()) {
            if (!(resource instanceof Credentials)) {
                String newId = prefix + "." + resource.resourceIdentifier();

                resource.resourceIdentifier(newId);
                paramFileScope.put(resource.resourceType() + "::" + newId, resource);
            }
        }
    }

    @Override
    public Object evaluate(Scope scope) {
        throw new UnsupportedOperationException();
    }

    public String getName() {
        return name;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {

    }

}
