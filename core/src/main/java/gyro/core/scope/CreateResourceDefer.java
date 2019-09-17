package gyro.core.scope;

class CreateResourceDefer extends Defer {

    public CreateResourceDefer(Defer cause, String type, String name) {
        super(
            null,
            String.format("Can't create @|bold %s|@ @|bold %s|@ resource!", type, name),
            cause);
    }

}
