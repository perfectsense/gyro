package gyro.lang.ast.query;

import gyro.lang.Resource;
import gyro.lang.ast.scope.Scope;
import gyro.parser.antlr4.BeamParser;

import java.util.List;

public class FieldQuery extends Query {

    private String value;

    public FieldQuery(BeamParser.FieldQueryContext context) {
        this.value = context.getText();
    }

    public String getValue() {
        return value;
    }

    @Override
    public List<Resource> evaluate(String type, Scope scope, List<Resource> resources) throws Exception {
        throw new UnsupportedOperationException();
    }
}
