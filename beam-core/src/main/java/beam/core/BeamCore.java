package beam.core;

import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiff;
import beam.core.diff.ResourceName;
import beam.lang.Resource;
import beam.lang.FileBackend;
import beam.lang.ast.scope.FileScope;
import com.psddev.dari.util.ThreadLocalStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeamCore {

    private final Map<String, Class<? extends Resource>> resourceTypes = new HashMap<>();

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

    public List<ResourceDiff> diff(FileScope currentScope, FileScope pendingScope, boolean refresh) throws Exception {
        ResourceDiff diff = new ResourceDiff(currentScope, pendingScope);
        diff.setRefresh(refresh);
        diff.diff();

        List<ResourceDiff> diffs = new ArrayList<>();
        diffs.add(diff);

        return diffs;
    }

    public Set<ChangeType> writeDiffs(List<ResourceDiff> diffs) {
        Set<ChangeType> changeTypes = new HashSet<>();
        for (ResourceDiff diff : diffs) {
            if (!diff.hasChanges()) {
                continue;
            }

            for (ResourceChange change : diff.getChanges()) {
                ChangeType type = change.getType();
                List<ResourceDiff> changeDiffs = change.getDiffs();

                if (type == ChangeType.KEEP) {
                    boolean hasChanges = false;

                    for (ResourceDiff changeDiff : changeDiffs) {
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

    public void setChangeable(List<ResourceDiff> diffs) {
        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                change.setChangeable(true);
                setChangeable(change.getDiffs());
            }
        }
    }

    public void createOrUpdate(List<ResourceDiff> diffs) {
        setChangeable(diffs);

        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                ChangeType type = change.getType();

                if (type == ChangeType.CREATE || type == ChangeType.UPDATE) {
                    execute(change);
                }

                createOrUpdate(change.getDiffs());
            }
        }
    }

    public void delete(List<ResourceDiff> diffs) {
        setChangeable(diffs);

        for (ResourceDiff diff : diffs) {
            for (ResourceChange change : diff.getChanges()) {
                delete(change.getDiffs());

                if (change.getType() == ChangeType.DELETE) {
                    execute(change);
                }
            }
        }
    }

    public void execute(ResourceChange change) {
        ChangeType type = change.getType();

        if (type == ChangeType.KEEP || type == ChangeType.REPLACE || change.isChanged()) {
            return;
        }

        Set<ResourceChange> dependencies = change.dependencies();

        if (dependencies != null && !dependencies.isEmpty()) {
            for (ResourceChange d : dependencies) {
                execute(d);
            }
        }

        BeamCore.ui().write("Executing: ");
        writeChange(change);
        Resource resource = change.executeChange();

        FileScope state = resource.scope().getFileScope().getState();
        FileBackend backend = resource.scope().getFileScope().getFileBackend();

        ResourceName nameAnnotation = resource.getClass().getAnnotation(ResourceName.class);
        boolean isSubresource = nameAnnotation != null && !nameAnnotation.parent().equals("");

        if (type == ChangeType.DELETE) {
            if (isSubresource) {
                //Resource parent = resource.parentResource();
                //parent.removeSubresource(resource);

                //stateNode.putResource(parent);
            } else {
                state.getFileScope().getResources().remove(resource.resourceIdentifier());
            }
        } else {
            if (isSubresource) {
                // Save parent resource when current resource is a subresource.
                //Resource parent = resource.parentResource();
                //stateNode.putResource(parent);
            } else {
                state.getFileScope().getResources().put(resource.resourceIdentifier(), resource);
            }
        }

        BeamCore.ui().write(" OK\n");
        backend.save(state);

        for (FileScope importedScope : state.getImports()) {
            backend.save(importedScope);
        }
    }

    private void writeChange(ResourceChange change) {
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
