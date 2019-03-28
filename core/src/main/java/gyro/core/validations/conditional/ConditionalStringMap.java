package gyro.core.validations.conditional;

public @interface ConditionalStringMap {
    String[] values() default {};

    boolean isDefault() default false;
}
