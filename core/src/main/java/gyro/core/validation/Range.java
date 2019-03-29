package gyro.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@AnnotationProcessorClass(RangeValidator.class)
@Repeatable(Ranges.class)
public @interface Range {
    double low();

    double high();

    String message() default "Valid %s should be in the range of [ %s - %s ].";
}
