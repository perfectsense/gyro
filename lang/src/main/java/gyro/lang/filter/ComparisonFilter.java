package gyro.lang.filter;

import gyro.lang.ast.Node;
import gyro.parser.antlr4.GyroParser;

public class ComparisonFilter extends Filter {

    private final String operator;
    private final String key;
    private final Node value;

    public static final String EQUALS_OPERATOR = "=";
    public static final String NOT_EQUALS_OPERATOR = "!=";

    public ComparisonFilter(GyroParser.ComparisonFilterContext context) {
        this.operator = context.comparisonOperator().getText();
        this.key = context.IDENTIFIER().getText();
        this.value = Node.create(context.value().getChild(0));
    }

    public String getOperator() {
        return operator;
    }

    public String getKey() {
        return key;
    }

    public Node getValue() {
        return value;
    }

    @Override
    public <C, R> R accept(FilterVisitor<C, R> visitor, C context) {
        return visitor.visitComparison(this, context);
    }

}
