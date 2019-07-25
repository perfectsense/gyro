package gyro.core.resource;

import gyro.core.GyroException;
import gyro.core.Wait;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class WaitForDirective extends DirectiveProcessor<Scope> {

    @Override
    public String getName() {
        return "wait-for";
    }

    @Override
    public void process(Scope scope, DirectiveNode node) {
        List<Object> arguments = evaluateDirectiveArguments(scope, node, 1, 1);
        Object argument = arguments.get(0);
        if (!(argument instanceof Boolean)) {
            throw new GyroException(String.format(
                "@|bold %s|@ is not a @|bold Boolean|@ condition!",
                argument));
        }

        Integer atMost = 60;
        Integer checkEvery = 10;
        TimeUnit timeUnit = TimeUnit.SECONDS;

        Scope bodyScope = evaluateBody(scope, node);
        if (bodyScope.containsKey("at-most")) {
            atMost = (Integer) bodyScope.remove("at-most");
        }

        if (bodyScope.containsKey("check-every")) {
            checkEvery = (Integer) bodyScope.remove("check-every");
        }

        if (bodyScope.containsKey("time-unit")) {
            timeUnit = TimeUnit.valueOf((String) bodyScope.remove("time-unit"));
        }

        if (!bodyScope.isEmpty()) {
            throw new GyroException(String.format(
                "@|bold %s|@ is not a valid field!",
                bodyScope.keySet().stream().findFirst().get()));
        }

        Wait.atMost(atMost, timeUnit)
            .checkEvery(checkEvery, timeUnit)
            .until(() -> {
                scope.getRootScope().findResources().forEach(DiffableInternals::refresh);
                return (Boolean) evaluateDirectiveArguments(scope, node, 1, 1).get(0);
            });
    }

}
