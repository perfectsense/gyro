package gyro.core.validations.conditional;

public @interface ConditionalStringList {
    String[] values() default {};

    boolean isDefault() default false;
}
