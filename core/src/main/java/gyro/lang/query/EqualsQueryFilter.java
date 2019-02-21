package gyro.lang.query;

import gyro.core.diff.DiffableType;
import gyro.lang.Resource;

import java.util.Objects;

public class EqualsQueryFilter extends ComparisonQueryFilter {

    public EqualsQueryFilter(String field, String value) {
        super(field, value);
    }

    @Override
    public boolean matches(Resource resource) {
        return Objects.equals(
            DiffableType.getInstance(resource.getClass()).getFieldByBeamName(getField()).getValue(resource),
            getValue()
        );
    }

}
