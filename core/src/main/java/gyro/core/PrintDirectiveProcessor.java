package gyro.core;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

import java.util.List;

@Type("print")
public class PrintDirectiveProcessor extends DirectiveProcessor<Scope> {

    @Override
    public void process(Scope scope, DirectiveNode node) {
        validateArguments(node, 1, 1);
        Object argument = getArgument(scope, node, Object.class, 0);

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