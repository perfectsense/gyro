package beam.diff;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.psddev.dari.util.Lazy;

public abstract class Change<A> {

    private final ChangeType type;
    private final A currentAsset;
    private final List<Diff<?, ?, ?>> diffs = new ArrayList<>();
    private boolean changeable;
    private boolean changed;

    private final Lazy<A> changedAsset = new Lazy<A>() {

        @Override
        public final A create() throws Exception {
            A asset = change();
            changed = true;

            return asset;
        }
    };

    protected Change(ChangeType type, A currentAsset) {
        this.type = type;
        this.currentAsset = currentAsset;
    }

    public ChangeType getType() {
        return type;
    }

    public A getCurrentAsset() {
        return currentAsset;
    }

    public List<Diff<?, ?, ?>> getDiffs() {
        return diffs;
    }

    public boolean isChangeable() {
        return changeable;
    }

    public void setChangeable(boolean changeable) {
        this.changeable = changeable;
    }

    public boolean isChanged() {
        return changed;
    }

    public Set<Change<?>> dependencies() {
        return null;
    }

    protected abstract A change() throws Exception;

    public A getChangedAsset() {
        if (isChangeable()) {
            return changedAsset.get();

        } else {
            throw new IllegalStateException("Can't change yet!");
        }
    }
}
