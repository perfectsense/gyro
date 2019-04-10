package gyro.lang.ast.value;

public class ValueReferenceException extends Exception {

    private final String key;

    public ValueReferenceException(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
