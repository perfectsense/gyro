package beam.lang;

import java.util.Objects;

public class ResourceKey {

    private String type;
    private String identifier;

    public ResourceKey(String type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ResourceKey that = (ResourceKey) o;
        return Objects.equals(type, that.type) && Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, identifier);
    }

    @Override
    public String toString() {
        return String.format("$(%s %s)", type, identifier);
    }
}
