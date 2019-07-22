package gyro.lang.filter;

import gyro.parser.antlr4.GyroParser;
import org.antlr.v4.runtime.tree.ParseTree;

public abstract class Filter {

    public static Filter create(ParseTree context) {
        Class<? extends ParseTree> cc = context.getClass();

        if (cc.equals(GyroParser.AndFilterContext.class)) {
            return new AndFilter((GyroParser.AndFilterContext) context);

        } else if (cc.equals(GyroParser.OrFilterContext.class)) {
            return new OrFilter((GyroParser.OrFilterContext) context);

        } else if (cc.equals(GyroParser.ComparisonFilterContext.class)) {
            return new ComparisonFilter((GyroParser.ComparisonFilterContext) context);
        }

        return null;
    }

    public abstract <C, R> R accept(FilterVisitor<C, R> visitor, C context);

}
