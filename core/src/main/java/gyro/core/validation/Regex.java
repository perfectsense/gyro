package gyro.core.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Repeatable(Regexes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@ValidatorClass(RegexValidator.class)
public @interface Regex {

    String value();

    String message() default "";

}
