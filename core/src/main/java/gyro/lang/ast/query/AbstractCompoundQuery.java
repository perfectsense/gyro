package gyro.lang.ast.query;

import gyro.parser.antlr4.BeamParser;

public abstract class AbstractCompoundQuery extends Query {

    private final Query left;
    private final Query right;

    public AbstractCompoundQuery(BeamParser.QueryExpressionContext context) {
        this.left = Query.create(context.getChild(0));
        this.right = Query.create(context.getChild(2));
    }

    public Query getLeft() {
        return left;
    }

    public Query getRight() {
        return right;
    }
}
