package gyro.lang.query;

import gyro.parser.antlr4.GyroParser;

import java.util.List;

public class AndQuery extends AbstractCompoundQuery {

    public AndQuery(GyroParser.AndQueryContext context) {
        super(context);
    }

    public AndQuery(List<Query> children) {
        super(children);
    }

    @Override
    public <C, R> R accept(QueryVisitor<C, R> visitor, C context) {
        return visitor.visitAnd(this, context);
    }

}
