package beam.core;

import beam.lang.BeamConfig;
import beam.lang.BeamContext;
import beam.lang.BeamContextKey;
import beam.lang.BeamResolvable;
import com.google.common.base.CaseFormat;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;

public class BeamObject extends BeamConfig {
    @Override
    public boolean resolve(BeamContext context) {
        boolean progress = super.resolve(context);
        for (BeamContextKey key : listContextKeys()) {
            if (key.getType() != null) {
                continue;
            }

            BeamResolvable referable = getReferable(key);
            Object value = referable.getValue();

            try {
                String keyId = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key.getId());
                BeanUtils.setProperty(this, keyId, value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {

            }
        }

        return progress;
    }
}
