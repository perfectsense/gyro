package gyro.core.validation.conditional;

public @interface ConditionalNumberList {
    double[] value() default {};

    boolean isDouble() default false;

    boolean isDefault() default false;
}
