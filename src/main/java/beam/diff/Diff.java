package beam.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.psddev.dari.util.CompactMap;

public abstract class Diff<I, A, C> {

    private final List<Change<?>> changes = new ArrayList<>();

    public List<Change<?>> getChanges() {
        return changes;
    }

    /**
     * Returns all assets that are currently running.
     *
     * @return May be {@code null} to represent an empty iterable.
     */
    public abstract Iterable<? extends A> getCurrentAssets() throws Exception;

    /**
     * Returns the unique ID that represents the given {@code asset}.
     *
     * @return Never {@code null}.
     */
    public abstract I getIdFromAsset(A asset) throws Exception;

    /**
     * Returns all configs that should be applied.
     *
     * @return May be {@code null} to represent an empty iterable.
     */
    public abstract Iterable<? extends C> getPendingConfigs() throws Exception;

    /**
     * Returns the unique ID that represents the given {@code config}.
     *
     * @return Never {@code null}.
     */
    public abstract I getIdFromConfig(C config) throws Exception;

    /**
     * Called when a new asset needs to be created based on the given
     * {@code config}.
     *
     * @param config Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public abstract Change<A> newCreate(C config) throws Exception;

    /**
     * Called when the given {@code asset} needs to be updated based on the
     * given {@code config}.
     *
     * @param config Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public abstract Change<A> newUpdate(A asset, C config) throws Exception;

    /**
     * Called when the given {@code asset} needs to be deleted.
     *
     * @param config Can't be {@code null}.
     * @return May be {@code null} to indicate no change.
     */
    public abstract Change<A> newDelete(A asset) throws Exception;

    public void diff() throws Exception {
        Map<I, A> currentAssetsById = new CompactMap<>();
        Iterable<? extends A> currentAssets = getCurrentAssets();

        if (currentAssets != null) {
            for (A asset : currentAssets) {
                currentAssetsById.put(getIdFromAsset(asset), asset);
            }
        }

        Iterable<? extends C> pendingConfigs = getPendingConfigs();

        if (pendingConfigs != null) {
            for (C config : pendingConfigs) {
                I id = getIdFromConfig(config);
                A asset = currentAssetsById.remove(id);
                Change<?> change = asset != null ? newUpdate(asset, config) : newCreate(config);

                if (change != null) {
                    changes.add(change);
                }
            }
        }

        if (currentAssets != null) {
            for (A asset : currentAssetsById.values()) {
                Change<?> change = newDelete(asset);

                if (change != null) {
                    changes.add(change);
                }
            }
        }
    }

    public boolean hasChanges() {
        List<Change<?>> changes = getChanges();

        for (Change<?> change : changes) {
            if (change.getType() != ChangeType.KEEP) {
                return true;
            }
        }

        for (Change<?> change : changes) {
            for (Diff<?, ?, ?> diff : change.getDiffs()) {
                if (diff.hasChanges()) {
                    return true;
                }
            }
        }

        return false;
    }
}
