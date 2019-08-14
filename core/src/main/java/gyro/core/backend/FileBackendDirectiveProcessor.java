package gyro.core.backend;

import com.google.common.base.CaseFormat;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.FileBackend;
import gyro.core.Reflections;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

public class FileBackendDirectiveProcessor extends DirectiveProcessor<RootScope> {

    @Override
    public String getName() {
        return "file-backend";
    }

    @Override
    public void process(RootScope scope, DirectiveNode node) throws Exception {
        validateArguments(node, 0, 2);
        String type = getArgument(scope, node, String.class, 0);
        String name = getArgument(scope, node, String.class, 1);

        Scope bodyScope = evaluateBody(scope, node);

        FileBackendsSettings settings = scope.getSettings(FileBackendsSettings.class);
        Class<? extends FileBackend> fileBackendClass = settings.getFileBackendsClasses().get(type);

        FileBackend fileBackend = Reflections.newInstance(fileBackendClass);
        fileBackend.setName(name);

        for (PropertyDescriptor property : Reflections.getBeanInfo(fileBackendClass).getPropertyDescriptors()) {

            Method setter = property.getWriteMethod();
            if (setter != null) {
                Reflections.invoke(setter, fileBackend, scope.convertValue(
                        setter.getGenericParameterTypes()[0],
                        bodyScope.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName()))));
            }
        }
        settings.getFileBackends().put(name, fileBackend);
    }

}
