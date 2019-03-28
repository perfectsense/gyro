package gyro.core.validations.conditional;

import gyro.core.validations.RepeatableAnnotationProcessorClass;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RepeatableAnnotationProcessorClass(ConditionalsProcessor.class)
public @interface Conditionals {
    Conditional[] value();
}
