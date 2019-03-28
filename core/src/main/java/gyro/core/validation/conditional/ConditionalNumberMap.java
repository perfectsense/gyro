package gyro.core.validation.conditional;

public @interface ConditionalNumberMap {
    double[] value() default {};

    boolean isDouble() default false;

    boolean isDefault() default false;
}
