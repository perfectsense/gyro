package gyro.plugin.validation.annotations.number;

import gyro.plugin.validation.annotations.AnnotationProcessorClass;
import gyro.plugin.validation.annotations.GyroValidation;
import gyro.plugin.validation.annotations.number.validators.ListLongValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@AnnotationProcessorClass(ListLongValidator.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@GyroValidation
public @interface ListLong {
    long [] value();

    String message() default "Valid long values %s.";
}
