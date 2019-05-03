package gyro.lang.query;

import gyro.parser.antlr4.GyroParser;

import java.util.List;

public class OrQuery extends AbstractCompoundQuery {

    public OrQuery(GyroParser.OrQueryContext context) {
        super(context);
    }

    public OrQuery(List<Query> children) {
        super(children);
    }

    @Override
    public <C, R> R accept(QueryVisitor<C, R> visitor, C context) {
        return visitor.visitOr(this, context);
    }

}
