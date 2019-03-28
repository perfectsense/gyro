package gyro.core.validations.conditional;

public @interface ConditionalString {
    String[] values() default {};

    boolean isDefault() default false;
}
