package beam.core;

import beam.lang.BeamBlock;
import beam.lang.BeamContextKey;
import beam.lang.BeamResolvable;
import beam.lang.types.BeamValue;
import com.google.common.base.CaseFormat;
import org.apache.commons.beanutils.BeanUtils;

import java.lang.reflect.InvocationTargetException;

public class BeamValidatedBlock extends BeamBlock {

    public void populate() {
        String id = getParameters().get(0).getValue().toString();
        for (BeamContextKey key : keys()) {
            if (key.getType() != null) {
                continue;
            }

            BeamResolvable referable = get(key);
            Object value = referable.getValue();

            try {
                String keyId = CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, key.getId());
                if (!BeanUtils.describe(this).containsKey(keyId)) {
                    if (referable instanceof BeamValue) {
                        BeamValue beamValue = (BeamValue) referable;
                        if (beamValue.getLine() != null) {
                            BeamCore.validationException().addValidationError(
                                String.format("%s '%s' at line %s => %s is not a valid field.", getType(), id, beamValue.getLine(), key.getId()));
                        }
                    }
                }

                BeanUtils.setProperty(this, keyId, value);
            } catch (IllegalArgumentException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                // Ignoring errors from setProperty
            }
        }
    }
}
