package gyro.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@AnnotationProcessorClass(AllowedNumbersValidator.class)
public @interface AllowedNumbers {
    double [] value();

    boolean isDouble() default false;

    String message() default "Valid number should be one of %s.";
}
