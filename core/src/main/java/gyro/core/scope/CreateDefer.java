package gyro.core.scope;

class CreateDefer extends Defer {

    private final String key;

    public CreateDefer(Defer cause, String type, String name) {
        super(
            null,
            String.format("Can't create @|bold %s|@ @|bold %s|@ resource!", type, name),
            cause);

        this.key = type + "::" + name;
    }

    public String getKey() {
        return key;
    }

}
