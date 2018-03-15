package beam.core.diff;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResourceDiffProperty {

    public boolean updatable() default false;

    public boolean mapSummary() default false;

    public boolean nullable() default false;
}
