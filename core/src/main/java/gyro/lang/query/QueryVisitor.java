package gyro.lang.query;

public interface QueryVisitor<C, R> {

    default R visit(Query query, C context) {
        return query.accept(this, context);
    }

    R visitAnd(AndQuery query, C context);

    R visitComparison(ComparisonQuery query, C context);

    R visitFound(FoundQuery query, C context);

    R visitOr(OrQuery query, C context);

}
