package gyro.plugin.validation.annotations.string.validators;

import com.psddev.dari.util.ObjectUtils;
import gyro.plugin.validation.annotations.AnnotationProcessor;
import gyro.plugin.validation.annotations.string.RegexString;

public class RegexStringValidator implements AnnotationProcessor<RegexString, String> {
    private RegexString annotation;

    @Override
    public void initialize(RegexString annotation) {
        this.annotation = annotation;
    }

    @Override
    public boolean isValid(Object value) {
        if (value == null) {
            return false;
        }

        String valueCheck = (String) value;

        String regexValue = annotation.value();

        return valueCheck.matches(regexValue);
    }

    @Override
    public String getMessage() {
        return String.format(annotation.message(), ObjectUtils.isBlank(annotation.display())
            ? annotation.value() : annotation.display());
    }
}
