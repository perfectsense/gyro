package gyro.core.resource;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import gyro.core.GyroUI;
import gyro.core.diff.ChangeProcessor;
import gyro.core.scope.State;
import org.apache.commons.lang.time.StopWatch;

public class ElapsedTimeChangeProcessor extends ChangeProcessor {

    private Map<Resource, StopWatch> stopWatches = new HashMap<>();
    private static final String ELAPSED_TIME_MESSAGE = " (elapsed time: @|green %s|@)";

    private synchronized StopWatch start(Resource resource) {
        StopWatch stopWatch = stopWatches.computeIfAbsent(resource, r -> new StopWatch());
        stopWatch.reset();
        stopWatch.start();

        return stopWatch;
    }

    private synchronized String stop(GyroUI ui, Resource resource) {
        StopWatch stopWatch = stopWatches.get(resource);
        if (stopWatch == null) {
            return "0ms";
        }

        stopWatch.stop();

        Duration duration = Duration.ofMillis(stopWatch.getTime());

        if (duration.getSeconds() <= 10) {
            return String.format("%dms", duration.toMillis());
        } else {
            return String.format("%dm%ds", duration.toMinutes(), (duration.getSeconds() - (duration.toMinutes() * 60)));
        }
    }

    @Override
    public void beforeRefresh(GyroUI ui, Resource resource) throws Exception {
        start(resource);
    }

    @Override
    public void afterRefresh(GyroUI ui, Resource resource) throws Exception {
        String elapsed = stop(ui, resource);
        ui.write("Refreshing @|magenta,bold %s|@ took: @|green %s|@\n", resource.primaryKey(), elapsed);
    }

    @Override
    public void beforeCreate(GyroUI ui, State state, Resource resource) throws Exception {
        start(resource);
    }

    @Override
    public void afterCreate(GyroUI ui, State state, Resource resource) throws Exception {
        String elapsed = stop(ui, resource);
        ui.write(ELAPSED_TIME_MESSAGE, elapsed);
    }

    @Override
    public void beforeUpdate(
        GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
        start(pending);
    }

    @Override
    public void afterUpdate(
        GyroUI ui, State state, Resource current, Resource pending, Set<DiffableField> changedFields) throws Exception {
        String elapsed = stop(ui, current);
        ui.write(ELAPSED_TIME_MESSAGE, elapsed);
    }

    @Override
    public void beforeDelete(GyroUI ui, State state, Resource resource) throws Exception {
        start(resource);
    }

    @Override
    public void afterDelete(GyroUI ui, State state, Resource resource) throws Exception {
        String elapsed = stop(ui, resource);
        ui.write(ELAPSED_TIME_MESSAGE, elapsed);
    }
}
