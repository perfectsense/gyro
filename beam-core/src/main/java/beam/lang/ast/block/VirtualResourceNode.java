package beam.lang.ast.block;

import java.util.List;
import java.util.stream.Collectors;

import beam.lang.BeamLanguageException;
import beam.lang.Credentials;
import beam.lang.Resource;
import beam.lang.ast.Node;
import beam.lang.ast.scope.FileScope;
import beam.lang.ast.scope.RootScope;
import beam.lang.ast.scope.Scope;

import static beam.parser.antlr4.BeamParser.VirtualResourceContext;

public class VirtualResourceNode extends BlockNode {

    private String name;
    private List<VirtualResourceParam> params;

    public VirtualResourceNode(VirtualResourceContext context) {
        super(context.virtualResourceBody()
                .stream()
                .map(b -> Node.create(b.getChild(0)))
                .collect(Collectors.toList()));

        name = context.virtualResourceName().IDENTIFIER().getText();

        params = context.virtualResourceParam()
                .stream()
                .map(VirtualResourceParam::new)
                .collect(Collectors.toList());
    }

    public void createResources(String prefix, Scope paramScope) throws Exception {
        FileScope paramFileScope = paramScope.getFileScope();
        RootScope vrScope = new RootScope(paramFileScope.getFile());

        for (VirtualResourceParam param : params) {
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
            if (value instanceof Resource) {
                Resource resource = (Resource) value;

                if (!(value instanceof Credentials)) {
                    resource.resourceIdentifier(prefix + "." + resource.resourceIdentifier());
                }

                paramFileScope.put(resource.resourceType() + "::" + resource.resourceIdentifier(), resource);
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
