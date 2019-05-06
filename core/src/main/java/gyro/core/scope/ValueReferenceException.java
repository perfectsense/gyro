package gyro.core.scope;

public class ValueReferenceException extends RuntimeException {

    private final String key;

    public ValueReferenceException(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
