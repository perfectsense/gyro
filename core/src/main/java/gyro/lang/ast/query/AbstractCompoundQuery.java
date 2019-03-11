package gyro.lang.ast.query;

import com.google.common.collect.ImmutableList;
import gyro.parser.antlr4.BeamParser.QueryExpressionContext;

import java.util.List;

public abstract class AbstractCompoundQuery extends Query {

    private final List<Query> children;

    public AbstractCompoundQuery(QueryExpressionContext context) {
        children = ImmutableList.of(
            Query.create(context.getChild(0)),
            Query.create(context.getChild(2)));
    }

    public List<Query> getChildren() {
        return children;
    }
}
