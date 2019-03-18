package gyro.core.validations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AnnotationProcessorClass(ConditionalNumberDependentValidator.class)
public @interface ConditionalNumberDependent {
    String selected();

    double[] values();

    String[] dependent();

    ValidationUtils.DependencyType type();

    String message() default "Field%s %s %s %s when Field %s is set%s.";
}
