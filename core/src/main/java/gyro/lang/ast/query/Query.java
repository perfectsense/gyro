package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

public abstract class Query {

    public static Query create(ParseTree context) {
        Class<? extends ParseTree> cc = context.getClass();

        if (cc.equals(BeamParser.AndQueryContext.class)) {
            return new AndQuery((BeamParser.AndQueryContext) context);

        } else if (cc.equals(BeamParser.OrQueryContext.class)) {
            return new OrQuery((BeamParser.OrQueryContext) context);

        } else if (cc.equals(BeamParser.ComparisonQueryContext.class)) {
            return new ComparisonQuery((BeamParser.ComparisonQueryContext) context);
        }

        return null;
    }

    public abstract List<Resource> evaluate(String type, Scope scope, List<Resource> resources) throws Exception;
}
