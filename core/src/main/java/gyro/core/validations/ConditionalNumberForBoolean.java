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
@AnnotationProcessorClass(ConditionalNumberForBooleanValidator.class)
@Repeatable(ConditionalNumberForBooleans.class)
public @interface ConditionalNumberForBoolean {
    String selected();

    double[] selectedValues();

    boolean isSelectedDouble() default false;

    String dependent();

    boolean dependentValue();

    ValidationUtils.DependencyType type();

    String message() default "Field %s%s is %s when Field %s is set%s.";
}
