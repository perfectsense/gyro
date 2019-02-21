package gyro.lang.query;

import gyro.lang.Resource;

public class AndQueryFilter extends QueryFilter {

    private QueryFilter leftFilter;
    private QueryFilter rightFilter;

    public AndQueryFilter(QueryFilter leftFilter, QueryFilter rightFilter) {
        this.leftFilter = leftFilter;
        this.rightFilter = rightFilter;
    }

    public QueryFilter getLeftFilter() {
        return leftFilter;
    }

    public QueryFilter getRightFilter() {
        return rightFilter;
    }

    @Override
    public boolean matches(Resource resource) {
        return false;
    }

}
