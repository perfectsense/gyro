package gyro.core.validations.conditional;

import gyro.core.validations.AnnotationProcessorClass;
import gyro.core.validations.ValidationUtils;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@AnnotationProcessorClass(ConditionalValidator.class)
@Repeatable(Conditionals.class)
public @interface Conditional {
    String source();

    ConditionalString sourceStringValues() default @ConditionalString(isDefault = true);

    ConditionalStringList sourceStringListValues() default @ConditionalStringList(isDefault = true);

    ConditionalStringMap sourceStringMapValues() default @ConditionalStringMap(isDefault = true);

    ConditionalNumber sourceNumberValues() default @ConditionalNumber(isDefault = true);

    ConditionalNumberList sourceNumberListValues() default @ConditionalNumberList(isDefault = true);

    ConditionalNumberMap sourceNumberMapValues() default @ConditionalNumberMap(isDefault = true);

    ConditionalBoolean sourceBooleanValues() default @ConditionalBoolean(isDefault = true);

    String dependent();

    ConditionalString dependentStringValues() default @ConditionalString(isDefault = true);

    ConditionalStringList dependentStringListValues() default @ConditionalStringList(isDefault = true);

    ConditionalStringMap dependentStringMapValues() default @ConditionalStringMap(isDefault = true);

    ConditionalNumber dependentNumberValues() default @ConditionalNumber(isDefault = true);

    ConditionalNumberList dependentNumberListValues() default @ConditionalNumberList(isDefault = true);

    ConditionalNumberMap dependentNumberMapValues() default @ConditionalNumberMap(isDefault = true);

    ConditionalBoolean dependentBooleanValues() default @ConditionalBoolean(isDefault = true);

    ValidationUtils.DependencyType dependencyType();

    String message() default "Field %s%s is %s when Field %s is set%s.";
}
