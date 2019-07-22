package gyro.core.control;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;
import gyro.util.CascadingMap;

public class ForDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public String getName() {
        return "for";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        List<Object> variables = evaluateDirectiveArguments(scope, node, 1, 0);
        List<Node> inArguments = validateOptionArguments(node, "in", 1, 1);
        Node inNode = inArguments.get(0);
        Object in = scope.getRootScope().getEvaluator().visit(inNode, scope);

        if (in == null) {
            return;
        }

        if (in instanceof List) {
            List<?> list = (List<?>) in;
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

        } else if (in instanceof Map) {
            String keyVariable = (String) variables.get(0);

            if (variables.size() > 1) {
                String valueVariable = (String) variables.get(1);

                for (Map.Entry<?, ?> entry : ((Map<?, ?>) in).entrySet()) {
                    Map<String, Object> values = new LinkedHashMap<>();

                    values.put(keyVariable, entry.getKey());
                    values.put(valueVariable, entry.getValue());
                    processBody(node, scope, values);
                }

            } else {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) in).entrySet()) {
                    Map<String, Object> values = new LinkedHashMap<>();

                    values.put(keyVariable, entry.getKey());
                    processBody(node, scope, values);
                }
            }

        } else {
            throw new GyroException(inNode, String.format(
                "Can't iterate over @|bold %s|@ which resolved to @|bold %s|@ because it's not a collection!",
                inNode,
                in));
        }
    }

    private void processBody(DirectiveNode node, Scope scope, Map<String, Object> values) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        Scope bodyScope = new Scope(scope, new CascadingMap<>(scope, values));

        evaluator.visitBody(node.getBody(), bodyScope);
        scope.getKeyNodes().putAll(bodyScope.getKeyNodes());
    }

}
