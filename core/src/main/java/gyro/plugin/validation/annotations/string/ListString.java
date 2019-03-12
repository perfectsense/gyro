package gyro.plugin.validation.annotations.string;

import gyro.plugin.validation.annotations.AnnotationProcessorClass;
import gyro.plugin.validation.annotations.GyroValidation;
import gyro.plugin.validation.annotations.string.validators.ListStringValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@AnnotationProcessorClass(ListStringValidator.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@GyroValidation
public @interface ListString {
    String [] value();

    String message() default "Valid values %s.";
}
