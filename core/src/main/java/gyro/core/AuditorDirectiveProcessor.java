package gyro.core;

import com.google.common.base.CaseFormat;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;

@Type("auditor")
public class AuditorDirectiveProcessor extends DirectiveProcessor<RootScope> {
    @Override
    public void process(RootScope scope, DirectiveNode node) {
        // might need some more arguments
        validateArguments(node, 2, 2);

        // create auditor based on type
        String type = getArgument(scope, node, String.class, 0);
        String name = getArgument(scope, node, String.class, 1);
        Scope bodyScope = evaluateBody(scope, node);

        AuditorSettings settings = scope.getSettings(AuditorSettings.class);
        Class auditorClass = settings.getAuditorClasses().get(type);
        GyroAuditor auditor = (GyroAuditor) Reflections.newInstance(auditorClass);

        for (PropertyDescriptor property : Reflections.getBeanInfo(auditorClass).getPropertyDescriptors()) {
            Method setter = property.getWriteMethod();

            if (setter != null) {
                Reflections.invoke(setter, auditor, scope.convertValue(
                        setter.getGenericParameterTypes()[0],
                        bodyScope.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName()))));
            }
        }

        Map<String, GyroAuditor> auditorMap = scope.getSettings(AuditorSettings.class).getAuditorMap();
        if (!auditorMap.containsKey(name)) {
            auditorMap.put(name, auditor);
        }
    }
}
