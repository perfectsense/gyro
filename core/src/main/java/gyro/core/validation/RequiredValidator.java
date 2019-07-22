package gyro.core.validation;

import com.psddev.dari.util.ObjectUtils;

public class RequiredValidator implements Validator<Required> {

    @Override
    public boolean isValid(Required annotation, Object value) {
        return !ObjectUtils.isBlank(value);
    }

    @Override
    public String getMessage(Required annotation) {
        return "Required";
    }

}
