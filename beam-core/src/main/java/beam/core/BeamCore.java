package beam.core;

import beam.core.diff.ChangeType;
import beam.core.diff.ResourceChange;
import beam.core.diff.ResourceDiff;
import beam.core.diff.ResourceName;
import beam.lang.BeamFile;
import beam.lang.BeamLanguageException;
import beam.lang.BeamVisitor;
import beam.lang.Modification;
import beam.lang.Node;
import beam.lang.Resource;
import beam.lang.StateBackend;
import beam.lang.listeners.ErrorListener;
import beam.lang.listeners.PluginLoadingListener;
import beam.lang.listeners.StateBackendLoadingListener;
import beam.lang.plugins.PluginLoader;
import beam.parser.antlr4.BeamLexer;
import beam.parser.antlr4.BeamParser;
import beam.parser.antlr4.BeamParser.BeamFileContext;
import com.psddev.dari.util.ThreadLocalStack;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.reflections.Reflections;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BeamCore {

    private final Map<String, Class<? extends Resource>> resourceTypes = new HashMap<>();

    private PluginLoadingListener pluginListener;
    private StateBackendLoadingListener stateListener;
    private Reflections reflections;
    private boolean parsingState;

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

    public Reflections reflections() {
        return reflections;
    }

    public boolean parsingState() {
        return parsingState;
    }

    public BeamFile parse(String path) throws IOException {
        return parse(path, false);
    }

    public BeamFile parse(String path, boolean state) throws IOException {
        parsingState = state;

        // Initial file parse loads state and providers.
        BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(path));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        BeamParser parser = new BeamParser(tokens);
        ErrorListener errorListener = new ErrorListener();
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        BeamVisitor visitor = new BeamVisitor(this, path);

        stateListener = new StateBackendLoadingListener(this, visitor);
        parser.addParseListener(stateListener);

        pluginListener = new PluginLoadingListener(visitor);
        parser.addParseListener(pluginListener);

        BeamFileContext context = parser.beamFile();

        if (errorListener.getSyntaxErrors() > 0) {
            throw new BeamLanguageException(errorListener.getSyntaxErrors() + " errors while parsing.");
        }

        for (PluginLoader pluginLoader : pluginListener.plugins()) {
            pluginLoader.load();
        }

        // Load initial configuration
        BeamFile fileNode = visitor.visitBeamFile(context);

        if (!fileNode.resolve()) {
            System.out.println("Unable to resolve config.");
        }

        executeModifications(fileNode);

        // Load state, assuming this isn't a state file itself.
        if (!path.endsWith(".state")) {
            StateBackend backend = stateListener.getStateBackend();
            try {
                BeamFile stateNode = backend.load(fileNode, this);
                stateNode.copyNonResourceState(fileNode);

                fileNode.state(stateNode);
            } catch (Exception ex) {
                throw new BeamLanguageException("Unable to load state.", ex);
            }
        }

        return fileNode;
    }

    public BeamFile parseImport(String path) throws IOException {
        BeamLexer lexer = new BeamLexer(CharStreams.fromFileName(path));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        BeamParser parser = new BeamParser(tokens);
        ErrorListener errorListener = new ErrorListener();
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        parser.addParseListener(stateListener);
        parser.addParseListener(pluginListener);

        BeamFileContext context = parser.beamFile();

        if (errorListener.getSyntaxErrors() > 0) {
            throw new BeamLanguageException(errorListener.getSyntaxErrors() + " errors while parsing.");
        }

        for (PluginLoader pluginLoader : pluginListener.plugins()) {
            pluginLoader.load();
        }

        // Load configuration
        BeamVisitor visitor = new BeamVisitor(this, path);
        BeamFile fileNode = visitor.visitBeamFile(context);

        if (!fileNode.resolve()) {
            System.out.println("Unable to resolve config.");
        }

        executeModifications(fileNode);

        // Load state, assuming this isn't a state file itself.
        if (!path.endsWith(".state")) {
            StateBackend backend = fileNode.stateBackend();
            try {
                BeamFile stateNode = backend.load(fileNode, this);
                stateNode.copyNonResourceState(fileNode);

                fileNode.state(stateNode);
            } catch (Exception ex) {
                throw new BeamLanguageException("Unable to load state.", ex);
            }
        }

        return fileNode;
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

        BeamFile stateNode = resource.fileNode().state();
        StateBackend backend = resource.fileNode().stateBackend();

        ResourceName nameAnnotation = resource.getClass().getAnnotation(ResourceName.class);
        boolean isSubresource = nameAnnotation != null && !nameAnnotation.parent().equals("");

        if (type == ChangeType.DELETE) {
            if (isSubresource) {
                Resource parent = resource.parentResource();
                parent.removeSubresource(resource);

                stateNode.putResource(parent);
            } else {
                stateNode.removeResource(resource);
            }
        } else {
            if (isSubresource) {
                // Save parent resource when current resource is a subresource.
                Resource parent = resource.parentResource();
                stateNode.putResource(parent);
            } else {
                stateNode.putResource(resource);
            }
        }

        BeamCore.ui().write(" OK\n");
        backend.save(stateNode);

        for (BeamFile importNode : stateNode.imports().values()) {
            backend.save(importNode);
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

    private void executeModifications(BeamFile beamFile) {
        if (!parsingState()) {
            reflections = new Reflections("beam", getClass().getClassLoader(), PluginLoader.classLoader());

            for (Modification modification : beamFile.modifications()) {
                for (Resource resource : beamFile.resources()) {
                    Class parent = resource.getClass();
                    while (parent != java.lang.Object.class) {
                        if (modification.modifies().contains(parent.getName())) {
                            modification.modify(resource);
                            break;
                        }

                        parent = parent.getSuperclass();
                    }
                }
            }
        }
    }

}
