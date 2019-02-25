package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

public abstract class Query {

    public static Query create(ParseTree context) {
        Class<? extends ParseTree> cc = context.getClass();

        if (cc.equals(BeamParser.QueryAndExpressionContext.class)) {
            return new AndQuery((BeamParser.QueryAndExpressionContext) context);

        } else if (cc.equals(BeamParser.QueryOrExpressionContext.class)) {
            return new OrQuery((BeamParser.QueryOrExpressionContext) context);

        } else if (cc.equals(BeamParser.QueryComparisonExpressionContext.class)) {
            return new ComparisonQuery((BeamParser.QueryComparisonExpressionContext) context);

        } else if (cc.equals(BeamParser.QueryFieldValueContext.class)) {
            return new FieldValueQuery((BeamParser.QueryFieldValueContext) context);

        }

        return null;
    }


    public abstract Object evaluate(Resource resource, List<Resource> resources, Scope scope) throws Exception;

}
