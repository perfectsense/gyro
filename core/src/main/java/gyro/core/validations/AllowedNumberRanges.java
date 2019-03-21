package gyro.core.validations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RepeatableAnnotationProcessorClass(AllowedNumberRangesProcessor.class)
public @interface AllowedNumberRanges {
    AllowedNumberRange[] value();

    String message() default "Valid number should be in one of these ranges %s.";
}
