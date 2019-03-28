package gyro.core.validation.conditional;

public @interface ConditionalStringList {
    String[] values() default {};

    boolean isDefault() default false;
}
