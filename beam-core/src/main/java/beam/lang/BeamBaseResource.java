package beam.lang;

import beam.core.BeamException;
import com.google.common.base.CaseFormat;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public abstract class BeamBaseResource extends ResourceNode {

    @Override
    protected final void syncInternalToProperties() {
        for (String key : keys()) {
            Object value = get(key).getValue();

            try {
                String convertedKey = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key);

                if (!BeanUtils.describe(this).containsKey(convertedKey)) {
                    ValueNode valueNode = get(key);
                    String message = String.format("invalid attribute '%s' found on line %s", key, valueNode.getLine());

                    throw new BeamException(message);
                }

                BeanUtils.setProperty(this, convertedKey, value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                // Ignoring errors from setProperty
            }
        }

        for (String subResourceField : subResources().keySet()) {
            List<ResourceNode> subResources = subResources().get(subResourceField);

            try {
                BeanUtils.setProperty(this, subResourceField, subResources);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
                // Ignoring errors from setProperty
                e.printStackTrace();
            }
        }
    }

}
