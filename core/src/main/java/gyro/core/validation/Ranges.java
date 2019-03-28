package gyro.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@RepeatableAnnotationProcessorClass(RangesProcessor.class)
public @interface Ranges {
    Range[] value();

    String message() default "Valid number should be in one of these ranges %s.";
}
