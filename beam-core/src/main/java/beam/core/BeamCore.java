package beam.core;

import beam.core.diff.ChangeType;
import beam.core.diff.Change;
import beam.core.diff.Diff;
import beam.lang.ast.scope.RootScope;
import beam.lang.ast.scope.State;
import com.psddev.dari.util.ThreadLocalStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class BeamCore {

    private static final ThreadLocalStack<BeamUI> UI = new ThreadLocalStack<>();

    public static BeamUI ui() {
        return UI.get();
    }

    public static void pushUi(BeamUI ui) {
        UI.push(ui);
    }

    public static BeamUI popUi() {
        return UI.pop();
    }

    public List<Diff> diff(RootScope currentScope, RootScope pendingScope, boolean refresh) throws Exception {
        Diff diff = new Diff(currentScope.findAllResources(), pendingScope.findAllResources());
        diff.setRefresh(refresh);
        diff.diff();

        List<Diff> diffs = new ArrayList<>();
        diffs.add(diff);

        return diffs;
    }

    public Set<ChangeType> writeDiffs(List<Diff> diffs) {
        Set<ChangeType> changeTypes = new HashSet<>();
        for (Diff diff : diffs) {
            if (!diff.hasChanges()) {
                continue;
            }

            for (Change change : diff.getChanges()) {
                ChangeType type = change.getType();
                List<Diff> changeDiffs = change.getDiffs();

                if (type == ChangeType.KEEP) {
                    boolean hasChanges = false;

                    for (Diff changeDiff : changeDiffs) {
                        if (changeDiff.hasChanges()) {
                            hasChanges = true;
                            break;
                        }
                    }

                    if (!hasChanges) {
                        continue;
                    }
                }

                changeTypes.add(type);
                writeChange(change);
                BeamCore.ui().write("\n");
                BeamCore.ui().indented(() -> changeTypes.addAll(writeDiffs(changeDiffs)));
            }
        }

        return changeTypes;
    }

    public void setChangeable(List<Diff> diffs) {
        for (Diff diff : diffs) {
            for (Change change : diff.getChanges()) {
                change.setChangeable(true);
                setChangeable(change.getDiffs());
            }
        }
    }

    public void createOrUpdate(State state, List<Diff> diffs) throws Exception {
        setChangeable(diffs);

        for (Diff diff : diffs) {
            for (Change change : diff.getChanges()) {
                ChangeType type = change.getType();

                if (type == ChangeType.CREATE || type == ChangeType.UPDATE) {
                    execute(state, change);
                }

                createOrUpdate(state, change.getDiffs());
            }
        }
    }

    public void delete(State state, List<Diff> diffs) throws Exception {
        setChangeable(diffs);

        for (ListIterator<Diff> i = diffs.listIterator(diffs.size()); i.hasPrevious();) {
            Diff diff = i.previous();

            for (ListIterator<Change> j = diff.getChanges().listIterator(diff.getChanges().size()); j.hasPrevious();) {
                Change change = j.previous();

                delete(state, change.getDiffs());

                if (change.getType() == ChangeType.DELETE) {
                    execute(state, change);
                }
            }
        }
    }

    public void execute(State state, Change change) throws Exception {
        ChangeType type = change.getType();

        if (type == ChangeType.KEEP || type == ChangeType.REPLACE || change.isChanged()) {
            return;
        }

        Set<Change> dependencies = change.dependencies();

        if (dependencies != null && !dependencies.isEmpty()) {
            for (Change d : dependencies) {
                execute(state, d);
            }
        }

        BeamCore.ui().write("Executing: ");
        writeChange(change);
        change.executeChange();
        BeamCore.ui().write(" OK\n");
        state.update(change);
    }

    private void writeChange(Change change) {
        switch (change.getType()) {
            case CREATE :
                BeamCore.ui().write("@|green + %s|@", change);
                break;

            case UPDATE :
                if (change.toString().contains("@|")) {
                    BeamCore.ui().write(" * %s", change);
                } else {
                    BeamCore.ui().write("@|yellow * %s|@", change);
                }
                break;

            case REPLACE :
                BeamCore.ui().write("@|blue * %s|@", change);
                break;

            case DELETE :
                BeamCore.ui().write("@|red - %s|@", change);
                break;

            default :
                BeamCore.ui().write(change.toString());
        }
    }

    public enum ResourceType {
        RESOURCE,
        VIRTUAL_RESOURCE,
        UNKNOWN
    }

}
