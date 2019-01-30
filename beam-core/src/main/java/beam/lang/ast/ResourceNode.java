package beam.lang.ast;

import beam.lang.BeamLanguageException;
import beam.lang.Resource;
import beam.lang.ast.scope.Scope;
import beam.lang.plugins.PluginLoader;
import beam.parser.antlr4.BeamParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResourceNode extends Node {

    private final String type;
    private final Node name;
    private final List<Node> body;

    public ResourceNode(BeamParser.ResourceContext context) {
        type = context.resourceType().IDENTIFIER().getText();

        if (context.resourceName() != null) {
            name = Node.create(context.resourceName().getChild(0));
        } else {
            name = null;
        }

        body = new ArrayList<>();
        for (BeamParser.ResourceBodyContext bodyContext : context.resourceBody()) {
            body.add(Node.create(bodyContext.getChild(0)));
        }
    }


    @Override
    public Object evaluate(Scope scope) {
        String n = null;

        if (name != null) {
            n = Optional.ofNullable(name.evaluate(scope))
                .map(Object::toString)
                .orElse(null);
        }

        Scope bodyScope = new Scope(scope);
        bodyScope.put("_resource_name", n);
        bodyScope.put("_resource_type", type);

        for (Node node : body) {
            node.evaluate(bodyScope);
        }

        if (n != null) {
            Resource resource = createResource(scope, type);
            resource.scope(bodyScope);
            resource.syncInternalToProperties();

            if (!scope.containsKey("_resource_name")) {
                scope.getResources().put(n, resource);
            }

            //scope.getCurrentResources().put(n, new Resource(type, n, null));
        } else {
            if ("plugin".equals(type)) {
                String artifact = (String) bodyScope.get("artifact");
                List<String> repositories = (List<String>) bodyScope.get("repositories");

                PluginLoader loader = new PluginLoader(scope, artifact, repositories);
                loader.load();

                List<PluginLoader> plugins = scope.getPlugins();
                plugins.add(loader);
            } else if ("state".equals(type)) {
                //scope.setStateBackend(new Resource(type, n, bodyScope));
            } else {
                String resourceType = String.format("%s::%s", scope.get("_resource_type"), type);
                Resource resource = createResource(scope, resourceType);
                resource.resourceType(type);
                resource.scope(bodyScope);
                resource.syncInternalToProperties();

                List<Resource> subresources = (List<Resource>) scope.computeIfAbsent("_subresources", s -> new ArrayList<>());
                subresources.add(resource);
            }
        }

        return null;
    }

    @Override
    public void buildString(StringBuilder builder, int indentDepth) {
        buildNewline(builder, indentDepth);
        builder.append(type);

        if (name != null) {
            builder.append(' ');
            builder.append(name);
        }

        buildBody(builder, indentDepth + 1, body);

        buildNewline(builder, indentDepth);
        builder.append("end");
    }

    private beam.lang.Resource createResource(Scope scope, String type) {
        Class klass = scope.getTypes().get(type);
        if (klass != null) {
            try {
                beam.lang.Resource resource = (beam.lang.Resource) klass.newInstance();
                resource.resourceType(type);

                return resource;
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new BeamLanguageException("Unable to instantiate " + klass.getClass().getSimpleName());
            }
        }

        throw new BeamLanguageException("Unknown resource type: " + type);
    }
}
