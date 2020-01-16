package gyro.core.validation;

import java.util.Collection;

import gyro.core.GyroException;
import gyro.core.resource.Diffable;

public class CollectionMinValidator implements Validator<CollectionMin> {

    @Override
    public boolean isValid(Diffable diffable, CollectionMin annotation, Object value) {
        if (value instanceof Collection) {
            return ((Collection<?>) value).size() >= annotation.value();
        } else {
            throw new GyroException("Invalid usage of '@CollectionMin' validation annotation. Can only be used on types that extend Collection.");
        }
    }

    @Override
    public String getMessage(CollectionMin annotation) {
        return String.format("Size of the collection must be greater than or equal to @|bold %d|@", annotation.value());
    }
}
