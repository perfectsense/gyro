package gyro.core.validations.conditional;

public @interface ConditionalNumberList {
    double[] values() default {};

    boolean isDouble() default false;

    boolean isDefault() default false;
}
