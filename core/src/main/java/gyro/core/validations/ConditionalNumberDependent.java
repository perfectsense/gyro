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
@AnnotationProcessorClass(ConditionalNumberDependentValidator.class)
@Repeatable(value = ConditionalNumberDependents.class)
public @interface ConditionalNumberDependent {
    String selected();

    double[] selectedValues() default {};

    boolean isSelectedDouble() default false;

    String dependent();

    double[] dependentValues() default {};

    boolean isDependentDouble() default false;

    ValidationUtils.DependencyType type();

    String message() default "Field %s%s is %s when Field %s is set%s.";
}
