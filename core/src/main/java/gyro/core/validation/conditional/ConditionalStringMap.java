package gyro.core.validation.conditional;

public @interface ConditionalStringMap {
    String[] value() default {};

    boolean isDefault() default false;
}
