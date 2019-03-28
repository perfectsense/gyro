package gyro.core.validation.conditional;

public @interface ConditionalString {
    String[] values() default {};

    boolean isDefault() default false;
}
