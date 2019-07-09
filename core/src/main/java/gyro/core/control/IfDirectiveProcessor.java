package gyro.core.control;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.resource.NodeEvaluator;
import gyro.core.resource.Scope;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.block.DirectiveSection;

public class IfDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public String getName() {
        return "if";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        if (processSection(scope, node.getArguments(), node.getBody())) {
            return;
        }

        for (DirectiveSection section : node.getSections()) {
            String name = section.getName();

            switch (name) {
                case "elseif" :
                case "elsif" :
                case "elif" :
                    if (processSection(scope, section.getArguments(), section.getBody())) {
                        return;
                    }

                    break;

                case "else" :
                    scope.getRootScope().getEvaluator().visitBody(section.getBody(), scope);
                    return;

                default :
                    throw new GyroException(String.format(
                        "@|bold -%s|@ is not a valid section within an @|bold @if|@ directive!",
                        name));
            }
        }
    }

    private boolean processSection(Scope scope, List<Node> arguments, List<Node> body) {
        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();
        List<Object> conditions = evaluator.visit(arguments, scope);

        if (conditions.size() != 1) {
            throw new GyroException("@|bold @if|@ directive requires exactly 1 condition!");
        }

        if (NodeEvaluator.test(conditions.get(0))) {
            evaluator.visitBody(body, scope);
            return true;

        } else {
            return false;
        }
    }

}
