package gyro.core.validation.conditional;

public @interface ConditionalString {
    String[] value() default {};

    boolean isDefault() default false;
}
