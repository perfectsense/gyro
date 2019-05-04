package gyro.lang.ast.value;

import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.query.Query;
import gyro.parser.antlr4.GyroParser;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceReferenceNode extends Node {

    private final String type;
    private final Node name;
    private final List<Query> queries;
    private final String path;

    public ResourceReferenceNode(GyroParser.ResourceReferenceContext context) {
        type = context.resourceType().getText();

        GyroParser.ReferenceNameContext rnc = context.referenceName();

        name = Optional.ofNullable(rnc.string())
            .map(Node::create)
            .orElseGet(() -> new ValueNode(rnc.getText()));

        queries = context.query()
            .stream()
            .map(Query::create)
            .collect(Collectors.toList());

        path = Optional.ofNullable(context.path())
            .map(GyroParser.PathContext::getText)
            .orElse(null);
    }

    public ResourceReferenceNode(String type, Node name, Collection<Query> queries, String path) {
        this.type = type;
        this.name = name;
        this.queries = ImmutableList.copyOf(queries);
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public Node getName() {
        return name;
    }

    public List<Query> getQueries() {
        return queries;
    }

    public String getPath() {
        return path;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitResourceReference(this, context);
    }

    @Override
    public String deferFailure() {
        return String.format("Unable to resolve resource reference %s %s%nResource '%s %s' is not defined.%n",
            this, getLocation(), type, name);
    }

}
