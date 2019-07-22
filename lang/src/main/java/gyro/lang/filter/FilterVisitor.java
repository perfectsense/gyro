package gyro.lang.filter;

public interface FilterVisitor<C, R> {

    default R visit(Filter filter, C context) {
        return filter.accept(this, context);
    }

    R visitAnd(AndFilter filter, C context);

    R visitComparison(ComparisonFilter filter, C context);

    R visitOr(OrFilter filter, C context);

}
