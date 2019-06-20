package gyro.core.finder;

import java.util.List;
import java.util.Objects;

import gyro.core.GyroException;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Scope;
import gyro.lang.query.AndQuery;
import gyro.lang.query.ComparisonQuery;
import gyro.lang.query.OrQuery;
import gyro.lang.query.QueryVisitor;

public class QueryEvaluator implements QueryVisitor<QueryContext, Boolean> {

    @Override
    public Boolean visitAnd(AndQuery query, QueryContext context) {
        return query.getChildren().stream().allMatch(c -> visit(c, context));
    }

    @Override
    public Boolean visitComparison(ComparisonQuery query, QueryContext context) {
        String operator = query.getOperator();
        Object x = NodeEvaluator.getValue(context.getValue(), query.getKey());
        Scope scope = context.getScope();
        Object y = scope.getRootScope().getEvaluator().visit(query.getValue(), scope);

        switch (operator) {
            case ComparisonQuery.EQUALS_OPERATOR :
                return roughlyEquals(x, y);

            case ComparisonQuery.NOT_EQUALS_OPERATOR :
                return !roughlyEquals(x, y);

            default :
                throw new GyroException(String.format(
                    "[%s] operator isn't supported!",
                    operator));
        }
    }

    @SuppressWarnings("unchecked")
    private boolean roughlyEquals(Object x, Object y) {
        if (Objects.equals(x, y)) {
            return true;

        } else if (x instanceof List) {
            if (!(y instanceof List)) {
                List<Object> xList = (List<Object>) x;

                if (xList.size() == 1) {
                    return roughlyEquals(xList.get(0), y);
                }
            }

        } else if (y instanceof List) {
            List<Object> yList = (List<Object>) y;

            if (yList.size() == 1) {
                return roughlyEquals(x, yList.get(0));
            }
        }

        return false;
    }

    @Override
    public Boolean visitOr(OrQuery query, QueryContext context) {
        return query.getChildren().stream().anyMatch(c -> visit(c, context));
    }

}
