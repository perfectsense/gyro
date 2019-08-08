package gyro.core.scope.converter;

import java.lang.reflect.Type;

import com.psddev.dari.util.ConversionFunction;
import com.psddev.dari.util.Converter;
import gyro.core.resource.DiffableField;
import gyro.core.resource.DiffableType;
import gyro.core.resource.Resource;
import gyro.util.Bug;

public class ResourceToIdObject implements ConversionFunction<Resource, Object> {

    @Override
    public Object convert(Converter converter, Type returnType, Resource resource) {
        DiffableType<Resource> type = DiffableType.getInstance(resource);
        DiffableField idField = type.getIdField();

        if (idField == null) {
            throw new Bug(String.format(
                "@|bold %s|@ type doesn't have an ID field!",
                type.getName()));
        }

        return converter.convert(
            returnType,
            DiffableType.getInstance(resource.getClass())
                .getIdField()
                .getValue(resource));
    }

}
