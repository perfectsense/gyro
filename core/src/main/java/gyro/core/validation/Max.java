package gyro.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@AnnotationProcessorClass(MaxValidator.class)
public @interface Max {
    double value();

    boolean isDouble() default false;

    String message() default "Minimum allowed number is %s.";
}
