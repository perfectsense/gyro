package gyro.core.validation.conditional;

public @interface ConditionalNumber {
    double[] value() default {};

    boolean isDouble() default false;

    boolean isDefault() default false;
}
