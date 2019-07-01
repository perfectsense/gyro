package gyro.core.control;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Scope;
import gyro.lang.ast.block.DirectiveNode;
import gyro.util.CascadingMap;

public class ForDirectiveProcessor extends DirectiveProcessor {

    @Override
    public String getName() {
        return "for";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        List<Object> variables = evaluateArguments(scope, node);

        if (variables.isEmpty()) {
            throw new GyroException("@for directive requires at least 1 variable declaration!");
        }

        List<Object> inArguments = evaluateOptionArguments(scope, node, "in");

        if (inArguments.size() != 1) {
            throw new GyroException("-in option only takes 1 argument!");
        }

        Object value = inArguments.get(0);

        if (value == null) {
            return;
        }

        if (value instanceof List) {
            List<?> list = (List<?>) value;
            int variablesSize = variables.size();
            int listSize = list.size();

            for (int i = 0; i < listSize; i += variablesSize) {
                Map<String, Object> values = new LinkedHashMap<>();

                for (int j = 0; j < variablesSize; j++) {
                    int k = i + j;

                    values.put(
                        (String) variables.get(j),
                        k < listSize
                            ? list.get(k)
                            : null);
                }

                processBody(node, scope, values);
            }

        } else if (value instanceof Map) {
            String keyVariable = (String) variables.get(0);

            if (variables.size() > 1) {
                String valueVariable = (String) variables.get(1);

                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    Map<String, Object> values = new LinkedHashMap<>();

                    values.put(keyVariable, entry.getKey());
                    values.put(valueVariable, entry.getValue());
                    processBody(node, scope, values);
                }

            } else {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                    Map<String, Object> values = new LinkedHashMap<>();

                    values.put(keyVariable, entry.getKey());
                    processBody(node, scope, values);
                }
            }

        } else {
            throw new GyroException("Can't iterate over a non-collection!");
        }
    }

    private void processBody(DirectiveNode node, Scope scope, Map<String, Object> values) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        Scope bodyScope = new Scope(scope, new CascadingMap<>(scope, values));

        evaluator.visitBody(node.getBody(), bodyScope);
        scope.getKeyNodes().putAll(bodyScope.getKeyNodes());
    }

}
