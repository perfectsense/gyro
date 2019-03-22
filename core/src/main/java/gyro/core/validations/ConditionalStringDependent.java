package gyro.core.validations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AnnotationProcessorClass(ConditionalStringDependentValidator.class)
@Repeatable(ConditionalStringDependents.class)
public @interface ConditionalStringDependent {
    String selected();

    String[] selectedValues() default {};

    String dependent();

    String[] dependentValues() default {};

    ValidationUtils.DependencyType type();

    String message() default "Field %s%s is %s when Field %s is set%s.";
}
