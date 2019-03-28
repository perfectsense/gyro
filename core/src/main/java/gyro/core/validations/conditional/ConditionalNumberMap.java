package gyro.core.validations.conditional;

public @interface ConditionalNumberMap {
    double[] values() default {};

    boolean isDouble() default false;

    boolean isDefault() default false;
}
