package gyro.core.validations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@AnnotationProcessorClass(AllowedNumberRangeValidator.class)
@Repeatable(AllowedNumberRanges.class)
public @interface AllowedNumberRange {
    double low();

    double high();

    boolean isDouble() default false;

    String message() default "Valid number should be in the range of [ %s - %s ].";
}
