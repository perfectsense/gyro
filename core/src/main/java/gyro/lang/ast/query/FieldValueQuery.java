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
    public Object evaluate(Resource resource, List<Resource> resources, Scope scope) throws Exception {
        return null;
    }

}
