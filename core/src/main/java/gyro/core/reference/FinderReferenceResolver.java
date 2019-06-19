package gyro.core.reference;

import java.util.List;

import gyro.core.GyroException;
import gyro.core.finder.Finder;
import gyro.core.finder.FinderSettings;
import gyro.core.finder.FinderType;
import gyro.core.resource.Resource;
import gyro.core.resource.Scope;
import gyro.lang.query.Query;

public class FinderReferenceResolver extends ReferenceResolver {

    @Override
    public String getName() {
        return "external-query";
    }

    @Override
    public Object resolve(Scope scope, List<Object> arguments, List<Query> queries) {
        String type = (String) arguments.remove(0);

        Class<? extends Finder<Resource>> finderClass = scope.getRootScope()
            .getSettings(FinderSettings.class)
            .getFinderClasses()
            .get(type);

        if (finderClass == null) {
            throw new GyroException(String.format(
                "[%s] resource doesn't support external queries!",
                type));
        }

        return FinderType.getInstance(finderClass)
            .newInstance(scope)
            .findAll();
    }

}
