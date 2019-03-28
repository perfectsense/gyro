package gyro.core.validation.conditional;

public @interface ConditionalBoolean {
    boolean value() default false;

    boolean isDefault() default false;
}
