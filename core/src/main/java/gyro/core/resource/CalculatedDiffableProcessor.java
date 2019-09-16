package gyro.core.resource;

import java.util.Set;
import java.util.stream.Collectors;

public class CalculatedDiffableProcessor extends DiffableProcessor {

    @Override
    public Set<String> process(Diffable diffable) {
        DiffableType<Diffable> type = DiffableType.getInstance(diffable);

        return type.getFields()
            .stream()
            .filter(DiffableField::isCalculated)
            .map(DiffableField::getName)
            .collect(Collectors.toSet());
    }

}
