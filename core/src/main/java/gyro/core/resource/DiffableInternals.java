package gyro.core.resource;

public final class DiffableInternals {

    private DiffableInternals() {
    }

    public static void setName(Diffable diffable, String name) {
        diffable.name = name;
    }

}
