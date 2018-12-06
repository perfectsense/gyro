package beam.lang;

public class BeamContextKey {

    private String type;

    private String id;

    public BeamContextKey(String id) {
        this(id, null);
    }

    public BeamContextKey(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BeamContextKey scopeContextKey(String scope) {
        return new BeamContextKey(String.format("%s.%s", scope, id), type);
    }

    @Override
    public String toString() {
        return type != null ? String.format("%s %s", type, id) : id;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        BeamContextKey otherKey = (BeamContextKey) other;
        boolean sameType = type != null ? type.equals(otherKey.type) : otherKey.type == null;
        boolean sameId = id != null ? id.equals(otherKey.id) : otherKey.id == null;
        return sameType && sameId;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }
}
