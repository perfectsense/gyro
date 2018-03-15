package beam.core.diff;

import java.util.List;

public class ResourceDiffKey {

    private final List<?> ids;

    public ResourceDiffKey(List<?> ids) {
        this.ids = ids;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;

        } else if (other instanceof ResourceDiffKey) {
            ResourceDiffKey otherKey = (ResourceDiffKey) other;
            int size = Math.min(ids.size(), otherKey.ids.size());
            boolean compared = false;

            for (int i = 0; i < size; ++ i) {
                Object value = ids.get(i);

                if (value != null) {
                    Object otherValue = otherKey.ids.get(i);

                    if (otherValue != null) {
                        compared = true;

                        if (!value.equals(otherValue)) {
                            return false;
                        }
                    }
                }
            }

            return compared;

        } else {
            return false;
        }
    }
}
