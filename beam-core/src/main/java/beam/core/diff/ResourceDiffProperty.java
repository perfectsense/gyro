package beam.core.diff;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResourceDiffProperty {

    public boolean updatable() default false;

    public boolean nullable() default false;

    public boolean subresource() default false;

}
