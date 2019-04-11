package gyro.lang.ast.block;

import java.util.List;
import java.util.stream.Collectors;

import gyro.lang.BeamLanguageException;
import gyro.lang.Credentials;
import gyro.lang.Resource;
import gyro.lang.ast.Node;
import gyro.lang.ast.scope.FileScope;
import gyro.lang.ast.scope.RootScope;
import gyro.lang.ast.scope.Scope;

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
        RootScope vrScope = new RootScope(paramFileScope.getFile());

        for (VirtualResourceParameter param : params) {
            String paramName = param.getName();

            if (!paramScope.containsKey(paramName)) {
                throw new BeamLanguageException(String.format("Required parameter '%s' is missing.", paramName));

            } else {
                vrScope.put(paramName, paramScope.get(paramName));
            }
        }

        RootScope paramRootScope = paramScope.getRootScope();

        vrScope.getResourceClasses().putAll(paramRootScope.getResourceClasses());

        paramRootScope.findAllResources()
                .stream()
                .filter(Credentials.class::isInstance)
                .forEach(c -> vrScope.put(c.resourceType() + "::" + c.resourceIdentifier(), c));

        for (Node node : body) {
            node.evaluate(vrScope);
        }

        for (Object value : vrScope.values()) {
            if (value instanceof Resource && !(value instanceof Credentials)) {
                Resource resource = (Resource) value;
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
