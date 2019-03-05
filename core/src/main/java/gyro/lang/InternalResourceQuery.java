package gyro.lang;

import gyro.core.BeamException;
import gyro.core.diff.DiffableField;
import gyro.core.diff.DiffableType;
import gyro.lang.ast.query.ComparisonQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class InternalResourceQuery<R extends Resource> extends ResourceQuery<R> {

    public InternalResourceQuery(String type, Class<R> resourceClass, String fieldName, String operator, Object value) {
        super(fieldName, operator, value);

        boolean validQuery = false;
        for (DiffableField field : DiffableType.getInstance(resourceClass).getFields()) {
            String key = field.getBeamName();
            if (key.equals(fieldName())) {
                validQuery = true;
            }
        }

        if (!validQuery) {
            throw new BeamException(String.format(
                "No such field [%s] defined %s!",
                fieldName(), type));
        }
    }

    @Override
    public final boolean external() {
        return false;
    }

    @Override
    public final List<R> query() {
        throw new IllegalStateException();
    }

    @Override
    public final List<R> filter(List<R> resources) {
        if (resources == null) {
            resources = new ArrayList<>();
        }

        if (ComparisonQuery.EQUALS_OPERATOR.equals(operator())) {
            resources = resources.stream()
                .filter(r -> Objects.equals(
                    DiffableType.getInstance(r.getClass()).getFieldByBeamName(fieldName()).getValue(r), value()))
                .collect(Collectors.toList());

        } else if (ComparisonQuery.NOT_EQUALS_OPERATOR.equals(operator())) {
            resources = resources.stream()
                .filter(r -> !Objects.equals(
                    DiffableType.getInstance(r.getClass()).getFieldByBeamName(fieldName()).getValue(r), value()))
                .collect(Collectors.toList());

        } else {
            throw new UnsupportedOperationException(String.format("Operator %s is not supported!", operator()));
        }

        return resources;
    }
}
