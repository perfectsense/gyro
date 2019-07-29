package gyro.core.backend;

import com.google.common.base.CaseFormat;
import gyro.core.FileBackend;
import gyro.core.Reflections;
import gyro.core.directive.DirectiveProcessor;
import gyro.core.scope.RootScope;
import gyro.core.scope.Scope;
import gyro.lang.ast.block.DirectiveNode;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.List;

public class FileBackendDirectiveProcessor extends DirectiveProcessor<RootScope> {
    @Override
    public String getName() {
        return "file-backend";
    }

    @Override
    public void process(RootScope scope, DirectiveNode node) throws Exception {
        List<Object> arguments = evaluateDirectiveArguments(scope,node,1,2);
        String type = (String) arguments.get(0);
        String name = arguments.size() == 1? "default" : (String)arguments.get(1);
        Scope bodyScope = evaluateBody(scope,node);

        FileBackendSettings settings = scope.getSettings(FileBackendSettings.class);
        Class<? extends FileBackend> fileBackendClass = settings.getFileBackendClasses().get(type);

        FileBackend fileBackend =  Reflections.newInstance(fileBackendClass);
        fileBackend.setName(name);

//        fileBackend.scope = scope;

        for (PropertyDescriptor property : Reflections.getBeanInfo(fileBackendClass).getPropertyDescriptors()) {

            Method setter = property.getWriteMethod();
            if (setter != null) {
                Reflections.invoke(setter, fileBackend, scope.convertValue(
                        setter.getGenericParameterTypes()[0],
                        bodyScope.get(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN, property.getName()))));
            }
        }

        fileBackend.getNameSpaces().stream()
                .map(ns -> name)
                .forEach(n -> settings.getFileBackendByName().put(n, fileBackend));
    }
}
