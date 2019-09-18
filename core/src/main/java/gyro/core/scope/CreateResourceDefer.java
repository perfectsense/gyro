package gyro.core.scope;

class CreateResourceDefer extends Defer {

    private final String id;

    public CreateResourceDefer(Defer cause, String type, String name) {
        super(
            null,
            String.format("Can't create @|bold %s|@ @|bold %s|@ resource!", type, name),
            cause);

        this.id = type + "::" + name;
    }

    public String getId() {
        return id;
    }

}
