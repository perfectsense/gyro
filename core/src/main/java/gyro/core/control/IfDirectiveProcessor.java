package gyro.core.control;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.Type;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.NodeEvaluator;
import gyro.core.scope.Scope;
import gyro.lang.Locatable;
import gyro.lang.ast.Node;
import gyro.lang.ast.block.DirectiveNode;
import gyro.lang.ast.block.DirectiveSection;

@Type("if")
public class IfDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public void process(Scope scope, DirectiveNode node) {
        if (processSection(scope, node, node.getArguments(), node.getBody())) {
            return;
        }

        for (DirectiveSection section : node.getSections()) {
            String name = section.getName();

            switch (name) {
                case "elseif" :
                case "elsif" :
                case "elif" :
                    if (processSection(scope, section, section.getArguments(), section.getBody())) {
                        return;
                    }

                    break;

                case "else" :
                    scope.getRootScope().getEvaluator().visitBody(section.getBody(), scope);
                    return;

                default :
                    throw new GyroException(section, String.format(
                        "@|bold -%s|@ isn't a valid section within an @|bold @if|@ directive!",
                        name));
            }
        }
    }

    private boolean processSection(Scope scope, Locatable parent, List<Node> arguments, List<Node> body) {
        int argumentsSize = arguments.size();

        if (argumentsSize != 1) {
            throw new GyroException(
                argumentsSize == 0 ? parent : arguments.get(1),
                "@|bold @if|@ directive requires exactly 1 condition!");
        }

        NodeEvaluator evaluator = scope.getRootScope().getEvaluator();

        if (NodeEvaluator.test(evaluator.visit(arguments.get(0), scope))) {
            evaluator.visitBody(body, scope);
            return true;

        } else {
            return false;
        }
    }

}
