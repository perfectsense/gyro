package gyro.plugin.validation.annotations.misc;

import gyro.plugin.validation.annotations.AnnotationProcessorClass;
import gyro.plugin.validation.annotations.GyroValidation;
import gyro.plugin.validation.annotations.misc.validators.RequiredValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@AnnotationProcessorClass(RequiredValidator.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@GyroValidation
public @interface Required {
    String message() default "Required field.";
}
