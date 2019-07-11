package gyro.lang.filter;

import gyro.parser.antlr4.GyroParser;

import java.util.List;

public class AndFilter extends AbstractCompoundFilter {

    public AndFilter(GyroParser.AndFilterContext context) {
        super(context);
    }

    public AndFilter(List<Filter> children) {
        super(children);
    }

    @Override
    public <C, R> R accept(FilterVisitor<C, R> visitor, C context) {
        return visitor.visitAnd(this, context);
    }

}
