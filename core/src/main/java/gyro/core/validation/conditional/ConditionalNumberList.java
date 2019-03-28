package gyro.core.validation.conditional;

public @interface ConditionalNumberList {
    double[] values() default {};

    boolean isDouble() default false;

    boolean isDefault() default false;
}
