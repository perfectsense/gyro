package gyro.core.scope.converter;

import java.lang.reflect.Type;
import java.util.Iterator;

import com.psddev.dari.util.ConversionFunction;
import com.psddev.dari.util.Converter;
import gyro.core.GyroException;

public class IterableToOne implements ConversionFunction<Iterable, Object> {

    @Override
    public Object convert(Converter converter, Type returnType, Iterable iterable) {
        Iterator<?> iterator = iterable.iterator();

        if (iterator.hasNext()) {
            Object first = iterator.next();

            if (iterator.hasNext()) {
                throw new GyroException(String.format(
                    "Can't have more than 1 item in @|bold %s|@!",
                    iterable));

            } else {
                return first;
            }

        } else {
            return null;
        }
    }

}
