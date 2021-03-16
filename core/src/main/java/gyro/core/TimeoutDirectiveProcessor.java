package gyro.core;

import com.psddev.dari.util.ObjectUtils;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.DiffableScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

@Type("timeout")
public class TimeoutDirectiveProcessor extends DirectiveProcessor<DiffableScope> {

    @Override
    public void process(DiffableScope diffableScope, DirectiveNode node) throws Exception {
        Scope bodyScope = evaluateBody(diffableScope, node);

        TimeoutSettings settings = diffableScope.getSettings(TimeoutSettings.class);

        TimeoutSettings.Action action = ObjectUtils.to(TimeoutSettings.Action.class, bodyScope.get("action"));

        if (action == null) {
            throw new GyroException(node, "action field is required on @timeout directive.");
        }

        TimeoutSettings.Timeout timeout = new TimeoutSettings.Timeout();

        timeout.setAtMost(ObjectUtils.to(String.class, bodyScope.get("at-most")));
        timeout.setCheckEvery(ObjectUtils.to(String.class, bodyScope.get("check-every")));

        if (bodyScope.containsKey("prompt")) {
            timeout.setPrompt(ObjectUtils.to(Boolean.class, bodyScope.get("prompt")));
        }

        if (bodyScope.containsKey("skip")) {
            timeout.setSkip(ObjectUtils.to(Boolean.class, bodyScope.get("skip")));
        }

        settings.getTimeouts().put(action, timeout);
    }
}
