package gyro.lang.ast.value;

import com.google.common.collect.ImmutableList;
import gyro.lang.ast.Node;
import gyro.lang.ast.NodeVisitor;
import gyro.lang.query.Query;
import gyro.parser.antlr4.GyroParser;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ReferenceNode extends Node {

    private final List<Node> arguments;
    private final List<Query> queries;

    public ReferenceNode(GyroParser.ReferenceContext context) {
        arguments = context.value()
            .stream()
            .map(Node::create)
            .collect(Collectors.toList());

        queries = context.query()
            .stream()
            .map(Query::create)
            .collect(Collectors.toList());
    }

    public ReferenceNode(List<Node> arguments, Collection<Query> queries) {
        this.arguments = ImmutableList.copyOf(arguments);
        this.queries = ImmutableList.copyOf(queries);
    }

    public List<Node> getArguments() {
        return arguments;
    }

    public List<Query> getQueries() {
        return queries;
    }

    @Override
    public <C, R> R accept(NodeVisitor<C, R> visitor, C context) {
        return visitor.visitReference(this, context);
    }

    @Override
    public String deferFailure() {
        return String.format(
            "Can't resolve reference! [%s]",
            getLocation());
    }

}
