package gyro.lang.filter;

import gyro.parser.antlr4.GyroParser;

import java.util.List;

public class OrFilter extends AbstractCompoundFilter {

    public OrFilter(GyroParser.OrFilterContext context) {
        super(context);
    }

    public OrFilter(List<Filter> children) {
        super(children);
    }

    @Override
    public <C, R> R accept(FilterVisitor<C, R> visitor, C context) {
        return visitor.visitOr(this, context);
    }

}
