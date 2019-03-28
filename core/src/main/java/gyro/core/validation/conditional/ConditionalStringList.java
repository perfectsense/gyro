package gyro.core.validation.conditional;

public @interface ConditionalStringList {
    String[] value() default {};

    boolean isDefault() default false;
}
