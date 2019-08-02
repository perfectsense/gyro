package gyro.core.scope.converter;

import java.lang.reflect.Type;

import com.psddev.dari.util.ConversionFunction;
import com.psddev.dari.util.Converter;
import gyro.core.resource.Diffable;
import gyro.core.resource.DiffableType;
import gyro.core.scope.DiffableScope;

public class DiffableScopeToDiffable implements ConversionFunction<DiffableScope, Diffable> {

    @Override
    @SuppressWarnings("unchecked")
    public Diffable convert(Converter converter, Type returnType, DiffableScope scope) {
        DiffableType type = DiffableType.getInstance((Class<? extends Diffable>) returnType);
        Diffable diffable = type.newDiffable(null, null, scope);

        diffable.initialize(scope);

        return diffable;
    }

}
