package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.GyroParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

public abstract class Query {

    public static Query create(ParseTree context) {
        Class<? extends ParseTree> cc = context.getClass();

        if (cc.equals(GyroParser.AndQueryContext.class)) {
            return new AndQuery((GyroParser.AndQueryContext) context);

        } else if (cc.equals(GyroParser.OrQueryContext.class)) {
            return new OrQuery((GyroParser.OrQueryContext) context);

        } else if (cc.equals(GyroParser.ComparisonQueryContext.class)) {
            return new ComparisonQuery((GyroParser.ComparisonQueryContext) context);
        }

        return null;
    }

    public abstract List<Resource> evaluate(String type, Scope scope, List<Resource> resources) throws Exception;
}
