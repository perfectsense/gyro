package gyro.core;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

import java.util.List;

public class PrintDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public String getName() {
        return "print";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        List<Object> arguments = evaluateDirectiveArguments(scope, node, 1, 1);
        Object argument = arguments.get(0);

        StringBuilder sb = new StringBuilder();
        if (argument instanceof List) {
            for (Object element : (List) argument) {
                sb.append(ObjectUtils.toJson(element, true));
                sb.append("\n");
            }
        } else {
            sb.append(ObjectUtils.toJson(argument, true));
            sb.append("\n");
        }

        print(sb.toString());
    }

    protected void print(String content) {
        GyroCore.ui().write(content);
        GyroCore.ui().write("\n");
    }

}