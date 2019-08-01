package gyro.core.scope.converter;

import java.lang.reflect.Type;

import com.psddev.dari.util.ConversionFunction;
import com.psddev.dari.util.Converter;
import gyro.core.resource.Resource;
import gyro.core.scope.RootScope;

public class IdObjectToResource implements ConversionFunction<Object, Resource> {

    private final RootScope root;

    public IdObjectToResource(RootScope root) {
        this.root = root;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Resource convert(Converter converter, Type returnType, Object id) {
        return root.findResourceById((Class<? extends Resource>) returnType, id);
    }

}
