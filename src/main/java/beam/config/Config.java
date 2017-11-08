package beam.config;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;

public abstract class Config {

    public Map<String, Object> toMap() {
        try {
            Map<String, Object> map = new CompactMap<String, Object>();
            BeanInfo info = Introspector.getBeanInfo(getClass());

            for (PropertyDescriptor desc : info.getPropertyDescriptors()) {
                Method readMethod = desc.getReadMethod();

                if (readMethod != null) {
                    map.put(desc.getName(), readMethod.invoke(this));
                }
            }

            return map;

        } catch (IllegalAccessException |
                IntrospectionException |
                InvocationTargetException error) {

            throw new IllegalStateException(error);
        }
    }

    @Override
    public String toString() {
        return ObjectUtils.toJson(toMap(), true);
    }
}
