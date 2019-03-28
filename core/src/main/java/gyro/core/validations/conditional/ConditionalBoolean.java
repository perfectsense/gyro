package gyro.core.validations.conditional;

public @interface ConditionalBoolean {
    boolean value() default false;

    boolean isDefault() default false;
}
