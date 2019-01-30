package beam.core;

import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiff;
import beam.core.diff.ResourceName;
import beam.lang.BeamFile;
import beam.lang.BeamLanguageException;
import beam.lang.Node;
import beam.lang.Resource;
import beam.lang.StateBackend;
import beam.lang.ast.FileScope;
import beam.lang.ast.ProcessScope;
import beam.lang.ast.Scope;
import beam.lang.listeners.ErrorListener;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParser.BeamFileContext;
import com.psddev.dari.util.ThreadLocalStack;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
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

    public void addResourceType(String key, Class<? extends Resource> extension) {
        resourceTypes.put(key, extension);
    }

    public boolean hasResourceType(String key) {
        return resourceTypes.containsKey(key);
    }

    public Class<? extends Node> getResourceType(String key) {
        return resourceTypes.get(key);
    }

    public ResourceType resourceType(String name) {
        if (getResourceType(name) != null) {
            return ResourceType.RESOURCE;
        }

        return ResourceType.UNKNOWN;
    }
      
    public FileScope parseScope(String path) throws IOException {
        return parseScope(path, false);
    }

    public FileScope parseScope(String path, boolean state) throws IOException {
        // Initial file parse loads state and providers.
        BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(path));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        BeamParser parser = new BeamParser(tokens);
        ErrorListener errorListener = new ErrorListener();
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        BeamFileContext context = parser.beamFile();

        beam.lang.ast.Node rootNode = beam.lang.ast.Node.create(context);
        ProcessScope processScope = new ProcessScope(null);
        FileScope rootScope = new FileScope(processScope);
        rootScope.setPath(path);
        rootNode.evaluate(rootScope);

        if (errorListener.getSyntaxErrors() > 0) {
            throw new BeamLanguageException(errorListener.getSyntaxErrors() + " errors while parsing.");
        }

        return rootScope;
    }

    public List<ResourceDiff> diff(FileScope pendingScope, boolean refresh) throws Exception {
        ResourceDiff diff = new ResourceDiff(pendingScope.getState(), pendingScope);
        diff.setRefresh(refresh);
        diff.diff();

        List<ResourceDiff> diffs = new ArrayList<>();
        diffs.add(diff);

        return diffs;
    }

    public List<ResourceDiff> diff(BeamFile pending, boolean refresh) throws Exception {
        ResourceDiff diff = new ResourceDiff(pending.state(), pending);
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

        FileScope state = resource.scope().getState();
        StateBackend backend = resource.scope().getStateBackend();

        ResourceName nameAnnotation = resource.getClass().getAnnotation(ResourceName.class);
        boolean isSubresource = nameAnnotation != null && !nameAnnotation.parent().equals("");

        if (type == ChangeType.DELETE) {
            if (isSubresource) {
                //Resource parent = resource.parentResource();
                //parent.removeSubresource(resource);

                //stateNode.putResource(parent);
            } else {
                state.getResources().remove(resource.resourceIdentifier());
            }
        } else {
            if (isSubresource) {
                // Save parent resource when current resource is a subresource.
                //Resource parent = resource.parentResource();
                //stateNode.putResource(parent);
            } else {
                state.getResources().put(resource.resourceIdentifier(), resource);
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
