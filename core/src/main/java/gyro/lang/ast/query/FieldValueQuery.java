package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
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
    public void evaluate(String type, Scope scope, List<Resource> resources) throws Exception {
        return;
    }

}
