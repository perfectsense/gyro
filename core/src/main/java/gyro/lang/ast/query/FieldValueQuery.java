package gyro.lang.ast.query;

import gyro.lang.ResourceQueryGroup;
import gyro.lang.ast.scope.Scope;
import gyro.lang.ast.value.ResourceReferenceNode;
import gyro.parser.antlr4.BeamParser.QueryFieldValueContext;

import java.util.List;

public class FieldValueQuery extends Query {

    private String value;

    public FieldValueQuery(QueryFieldValueContext context) {
        this.value = context.getText();
    }

    public String getValue() {
        return value;
    }

    @Override
    public List<ResourceQueryGroup> evaluate(Scope scope, String type, boolean external) throws Exception {
        return null;
    }

}
