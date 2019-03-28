package gyro.core.validation.conditional;

public @interface ConditionalStringMap {
    String[] values() default {};

    boolean isDefault() default false;
}
