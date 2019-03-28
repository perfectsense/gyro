package gyro.core.validations.conditional;

public @interface ConditionalNumber {
    double[] values() default {};

    boolean isDouble() default false;

    boolean isDefault() default false;
}
